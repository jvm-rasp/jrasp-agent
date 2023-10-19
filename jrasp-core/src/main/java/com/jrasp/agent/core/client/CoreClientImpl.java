package com.jrasp.agent.core.client;

import com.jrasp.agent.core.CoreConfigure;
import com.jrasp.agent.core.JvmSandbox;
import com.jrasp.agent.core.client.socket.RaspSocket;
import com.jrasp.agent.core.client.socket.RaspSocketImpl;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.monitor.Monitor;
import com.jrasp.agent.core.newlog.LogUtil;
import com.jrasp.agent.core.newlog.consumer.RemoteConsumer;
import com.jrasp.agent.core.client.handler.PacketHandler;
import com.jrasp.agent.core.client.handler.impl.*;
import com.jrasp.agent.core.task.HeartbeatTask;
import com.jrasp.agent.core.task.LogSendTask;
import com.jrasp.agent.core.task.PacketReadTask;
import com.jrasp.agent.core.task.ReconnectTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.*;

import com.jrasp.agent.core.client.packet.PacketType;

import static com.jrasp.agent.core.client.packet.PacketType.*;
import static java.lang.String.format;

/**
 * @author jrasp
 */
public class CoreClientImpl implements CoreClient {

    private static CoreClient INSTALL = new CoreClientImpl();

    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(5);

    public static Map<PacketType, PacketHandler> handlerMap = new ConcurrentHashMap<PacketType, PacketHandler>();

    private BlockingQueue<String> queue = new ArrayBlockingQueue<String>(500);

    private JvmSandbox jvmSandbox;

    private CoreConfigure cfg;

    private volatile RaspSocket raspSocket = null;

    private static boolean isInit = false;

    public static CoreClient getInstance() {
        return INSTALL;
    }

    @Override
    public void init(CoreConfigure cfg, Instrumentation inst) throws Exception {
        long start = System.currentTimeMillis();
        this.cfg = cfg;
        try {
            // init log
            LogUtil.init(queue, cfg.getLogsPath());
            LogUtil.info("LogUtil init success.");
            jvmSandbox = new JvmSandbox(cfg, inst);

            // init socket
            raspSocket = new RaspSocketImpl(cfg.getServerIp(), cfg.getServerPort());
            LogUtil.initRemoteConsumer(new RemoteConsumer(raspSocket));
            executorService.scheduleWithFixedDelay(new ReconnectTask(raspSocket), 60, 120, TimeUnit.SECONDS);
            LogUtil.info("RaspSocket init success.");

            registerHandler(jvmSandbox.getCoreModuleManager());

            // 日志发送的延迟时间可以拉长点，避免io占用过高，1～5s的延迟时可以接受的
            executorService.scheduleWithFixedDelay(new LogSendTask(queue), 2, 2, TimeUnit.SECONDS);

            // 处理daemon发送的命令
            executorService.scheduleWithFixedDelay(new PacketReadTask(raspSocket), 2, 2, TimeUnit.SECONDS);

            // 启动资源检测
            Monitor.Factory.init();
            // 初始化心跳
            executorService.scheduleAtFixedRate(new HeartbeatTask(), 30, 5 * 60, TimeUnit.SECONDS);

            // 初始化加载所有的模块
            try {
                jvmSandbox.getCoreModuleManager().reset();
            } catch (Throwable cause) {
                LogUtil.error("reset occur error when initializing.", cause);
            }

            LogUtil.info(String.format("client socket success init, bind to [%s:%s], cost time: %s ms",
                    cfg.getServerIp(), cfg.getServerPort(), System.currentTimeMillis() - start));

            // 容器场景使用 jattach $pid properties 读取jrasp.info系统参数
            String info = format("jrasp;%s;%s;%s;%s", cfg.getServerIp(), cfg.getServerPort(), cfg.getUuid(), cfg.getCoreVersion());
            System.setProperty("jrasp.info", info);
            writeAgentInitResult(info);
            isInit = true;
        } catch (Throwable cause) {
            LogUtil.error("client init failed.", cause);
            throw new IOException("client init failed.", cause);
        }
    }

    @Override
    public boolean isInit() {
        return isInit;
    }

    @Override
    public void close() throws Exception {
        if (isInit()) {
            if (null != jvmSandbox) {
                jvmSandbox.destroy();
                jvmSandbox = null;
            }
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
            handlerMap.clear();
            Monitor.Factory.clear();
            if (raspSocket != null) {
                raspSocket.close();
                raspSocket = null;
            }
            LogUtil.close();
            isInit = false;
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

    /**
     * 结果写入到run/pid/token
     */
    @Deprecated
    private void writeAgentInitResult(String info) {
        File file = new File(cfg.getRuntimeTokenPath());
        if (file.exists() && (!file.isFile() || !file.canWrite())) {
            throw new RuntimeException("write to result file : " + file + " failed.");
        } else {
            FileWriter fw = null;
            try {
                // 覆盖
                fw = new FileWriter(file, false);
                fw.append(info);
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

}