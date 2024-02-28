package com.jrasp.agent.core.server.socket;

import com.jrasp.agent.core.CoreConfigure;
import com.jrasp.agent.core.JvmSandbox;
import com.jrasp.agent.core.logging.Loggging;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.monitor.Monitor;
import com.jrasp.agent.core.server.CoreServer;
import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.impl.*;
import com.jrasp.agent.core.server.socket.handler.packet.Codec;
import com.jrasp.agent.core.server.socket.handler.packet.Packet;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;
import com.jrasp.agent.core.task.HeartbeatTask;
import com.jrasp.agent.core.util.Initializer;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jrasp.agent.core.server.socket.handler.packet.PacketType.*;
import static java.lang.String.format;

/**
 * BioServer 取代 jetty，仅用于传递配置参数，对性能没有要求
 *
 * @author jrasp
 */
public class SocketServer implements CoreServer {

    private static final String CLOSE_CALLER_CLASS = "com.jrasp.agent.core.server.socket.handler.impl.UninstallPacketHandler";

    private static final Logger LOGGER = Logger.getLogger(SocketServer.class.getName());

    private static volatile CoreServer coreServer;

    private final int requestThreadPool = 5;

    private final int socketTimeout = 1000 * 10;

    private volatile ServerSocket serverSocket;

    private JvmSandbox jvmSandbox;

    private CoreConfigure cfg;

    private final Initializer initializer = new Initializer(true);

    private ExecutorService requestService = Executors.newFixedThreadPool(requestThreadPool, new DefaultThreadFactory("rasp-command"));

    private ExecutorService serverService = Executors.newSingleThreadExecutor(new DefaultThreadFactory("rasp-server"));

    public Map<PacketType, PacketHandler> handlerMap = new ConcurrentHashMap<PacketType, PacketHandler>();

    private SocketServer() {
    }

    public static CoreServer getInstance() {
        if (null == coreServer) {
            synchronized (CoreServer.class) {
                if (null == coreServer) {
                    coreServer = new SocketServer();
                }
            }
        }
        return coreServer;
    }

    @Override
    public boolean isBind() {
        return initializer.isInitialized();
    }

    @Override
    public void bind(final CoreConfigure cfg, final Instrumentation inst) throws IOException {
        long start = System.currentTimeMillis() / 1000;
        this.cfg = cfg;
        try {
            initializer.initProcess(new Initializer.Processor() {
                @Override
                public void process() throws Throwable {
                    Loggging.init(cfg.getLogsPath());
                    LOGGER.info("server socket start init...");
                    jvmSandbox = new JvmSandbox(cfg, inst);
                    initHttpServer();
                    registerHandler(jvmSandbox.getCoreModuleManager());
                    runServer();
                }
            });

            // 启动资源监控
            Monitor.Factory.init();

            HeartbeatTask.start();

            // 初始化加载所有的模块
            try {
                jvmSandbox.getCoreModuleManager().reset();
            } catch (Throwable cause) {
                LOGGER.log(Level.WARNING, "reset occur error when initializing.", cause);
            }

            final InetSocketAddress local = getLocal();
            writeAgentInitResult(local);
            // 耗时瓶颈在于读写文件
            LOGGER.log(Level.INFO, "server socket success init, bind to [{0}:{1}], cost time: {2} ms", new Object[]{local.getHostName(), local.getPort(), System.currentTimeMillis() / 1000 - start});

        } catch (Throwable cause) {
            LOGGER.log(Level.WARNING, "initialize server failed.", cause);
            throw new IOException("server bind failed.", cause);
        }
    }

    @Override
    public void unbind() throws IOException {
        try {
            initializer.destroyProcess(new Initializer.Processor() {
                @Override
                public void process() throws Throwable {
                    if (serverSocket != null) {
                        requestService.shutdown();
                        serverService.shutdown();
                        handlerMap.clear(); // 加快GC
                        try {
                            serverSocket.close();
                            serverSocket = null;
                        } catch (IOException e) {
                            // IGNORE
                            LOGGER.log(Level.WARNING, "server socket closed failed.", e);
                        }
                    }
                }
            });

            HeartbeatTask.stop();
            Monitor.Factory.clear();
            LOGGER.log(Level.INFO, "server socket destroyed.");
        } catch (Throwable cause) {
            throw new IOException("unBind failed.", cause);
        }
    }

    @Override
    public InetSocketAddress getLocal() throws IOException {
        if (serverSocket != null) {
            return (InetSocketAddress) serverSocket.getLocalSocketAddress();
        }
        return null;
    }

    @Override
    public void destroy() {

        checkCaller(CLOSE_CALLER_CLASS);
        // 关闭JVM-SANDBOX
        /*
         * BUGFIX:
         * jvmRasp对象在一定情况下可能为空，导致这种情况的可能是destroy()调用发生在bind()方法调用之前
         * 所以这里做了一个判空处理，临时性解决这个问题。真正需要深究的是为什么destroy()竟然能在bind()之前被调用
         */
        if (null != jvmSandbox) {
            jvmSandbox.destroy();
        }

        // 关闭HTTP服务器
        if (isBind()) {
            try {
                unbind();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "server unBind failed when destroy.", e);
            }
        }

        // 关闭LOG
        LOGGER.info("rasp agent shutdown.");
        Loggging.destroy();
        System.out.println(format("%s  INFO [jrasp] %s ", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss").format(new Date()),
                "关闭RASP安全防护,技术支持:" + CoreConfigure.JRASP_SUPPORT_URL));
    }

    private void initHttpServer() throws IOException {

        final String serverIp = cfg.getServerIp();
        final int serverPort = cfg.getServerPort();

        // 如果IP:PORT已经被占用，则无法继续被绑定
        // 这里说明下为什么要这么无聊加个这个判断，让Jetty的Server.bind()抛出异常不是更好么？
        // 比较郁闷的是，如果这个端口的绑定是"SO_REUSEADDR"端口可重用的模式，那么这个server是能正常启动，但无法正常工作的
        // 所以这里必须先主动检查一次端口占用情况，当然了，这里也会存在一定的并发问题，BUT，我认为这种概率事件我可以选择暂时忽略
        if (isPortInUsing(serverIp, serverPort)) {
            throw new IllegalStateException(format("address[%s:%s] already in using, server bind failed.",
                    serverIp,
                    serverPort
            ));
        }

        this.serverSocket = new ServerSocket(serverPort, 50, InetAddress.getByName(serverIp));
        if (serverSocket != null) {
            System.setProperty("rasp.port", serverPort + "");
        }
    }

    private void registerHandler(DefaultCoreModuleManager coreModuleManager) {
        handlerMap.put(FLUSH, new FlushPacketHandler(coreModuleManager));
        handlerMap.put(UNINSTALL, new UninstallPacketHandler());
        handlerMap.put(INFO, new InfoPacketHandler());
        handlerMap.put(UPDATE, new UpdatePacketHandler(coreModuleManager));
        handlerMap.put(UNLOAD, new UnloadModuleHandler(coreModuleManager));
        handlerMap.put(ACTIVE, new ActiveModuleHandler(coreModuleManager));
        handlerMap.put(FROZEN, new FrozenModuleHandler(coreModuleManager));
        handlerMap.put(CONFIG, new UpdateConfigPacketHandler());
    }

    private void runServer() {
        serverService.submit(new Runnable() {
            @Override
            public void run() {
                while (serverSocket != null) {
                    try {
                        Socket socket = serverSocket.accept();
                        socket.setSoTimeout(socketTimeout);
                        requestService.submit(new RequestTask(socket));
                    } catch (Throwable t) {
                        // 关闭socket时这里会报错，先忽略
                        LOGGER.log(Level.FINE, "rasp server socket close");
                    }
                }
            }
        });
    }

    public class RequestTask implements Runnable {

        private Socket socket;

        public RequestTask(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            DataInputStream inputStream = null;
            DataOutputStream outputStream = null;
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                Packet request = Codec.INSTANCE.decode(inputStream);
                PacketHandler handler = handlerMap.get(request.getType());
                if (handler == null) {
                    throw new IllegalArgumentException(String.format("no handle packet. packet type -> [%s]", request.getType()));
                }
                Packet response = new Packet(request.getType(), handler.run(request.getData()));
                Codec.INSTANCE.encode(outputStream, response);
            } catch (Throwable t) {
                try {
                    if (outputStream != null) {
                        LOGGER.log(Level.WARNING, "error occurred when update module parameters", t);
                        Packet error = new Packet(PacketType.ERROR, t.getMessage());
                        Codec.INSTANCE.encode(outputStream, error);
                    }
                } catch (Exception e1) {
                    // ignore
                }
            } finally {
                closeQuietly(inputStream, outputStream, socket);
            }
        }
    }

    // 兼容jdk6
    private void closeQuietly(DataInputStream inputStream, DataOutputStream outputStream, Socket socket) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public class DefaultThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        public DefaultThreadFactory(String prefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);

            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public static boolean isPortInUsing(String host, int port) {
        Socket socket = null;
        try {
            final InetAddress Address = InetAddress.getByName(host);
            //建立一个Socket连接
            socket = new Socket(Address, port);
            return socket.isConnected();
        } catch (Throwable cause) {
            // ignore
        } finally {
            if (null != socket) {
                IOUtils.closeQuietly(socket);
            }
        }
        return false;
    }

    /**
     * 结果写入到run/pid/token
     *
     * @param local
     */
    private void writeAgentInitResult(InetSocketAddress local) {
        File file = new File(cfg.getRuntimeTokenPath());
        if (file.exists() && (!file.isFile() || !file.canWrite())) {
            throw new RuntimeException("write to result file : " + file + " failed.");
        } else {
            FileWriter fw = null;
            try {
                // 覆盖
                fw = new FileWriter(file, false);
                fw.append(format("jrasp;%s;%s\n", local.getHostName(), local.getPort()));
                fw.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (null != fw) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }


    private static void checkCaller(String callClass) {
        Thread currentThread = Thread.currentThread();
        if (currentThread != null) {
            StackTraceElement[] stackTrace = currentThread.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if (callClass.equals(stackTraceElement.getClassName())) {
                    return;
                }
            }
            throw new SecurityException("this method is not allowed to invoke. ");
        }
    }

}
