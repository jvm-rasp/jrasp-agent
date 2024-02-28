package com.jrasp.agent.launcher110;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 如果修改这个类，必须将类路径上的版本号抬升
 * 规范： 1.1.0 ----> 110
 */
public class AgentLauncher {

    private static final String CLOSE_CALLER_CLASS = "com.jrasp.agent.core.server.socket.handler.impl.UninstallPacketHandler";

    private static String getSandboxCoreJarPath(String sandboxHome, String coreVersion) {
        return sandboxHome + File.separatorChar + "lib" + File.separator + "jrasp-core-" + coreVersion + ".jar";
    }

    // JRASP默认主目录
    private static final String DEFAULT_JRASP_HOME
            = new File(AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParent();

    // 启动模式: agent方式加载
    private static final String LAUNCH_MODE_AGENT = "agent";

    // 启动模式: attach方式加载
    private static final String LAUNCH_MODE_ATTACH = "attach";

    // 启动默认
    private static String LAUNCH_MODE;

    // 全局持有ClassLoader用于隔离sandbox实现
    private static volatile Map<String/*NAMESPACE*/, RaspClassLoader> sandboxClassLoaderMap
            = new ConcurrentHashMap<String, RaspClassLoader>();

    private static final String CLASS_OF_CORE_CONFIGURE = "com.jrasp.agent.core.CoreConfigure";

    private static final String CLASS_OF_PROXY_CORE_SERVER = "com.jrasp.agent.core.server.ProxyCoreServer";


    /**
     * 启动加载
     *
     * @param featureString 启动参数
     *                      [namespace,prop]
     * @param inst          inst
     */
    public static void premain(String featureString, Instrumentation inst) {
        LAUNCH_MODE = LAUNCH_MODE_AGENT;
        install(toFeatureMap(featureString), inst);
    }

    /**
     * 动态加载
     *
     * @param featureString 启动参数
     *                      [namespace,token,ip,port,prop]
     * @param inst          inst
     */
    public static void agentmain(String featureString, Instrumentation inst) {
        LAUNCH_MODE = LAUNCH_MODE_ATTACH;
        install(toFeatureMap(featureString), inst);
    }

    private static synchronized ClassLoader loadOrDefineClassLoader(final String namespace,
                                                                    final String coreJar) throws Throwable {

        final RaspClassLoader classLoader;

        // 如果已经被启动则返回之前启动的ClassLoader
        if (sandboxClassLoaderMap.containsKey(namespace)
                && null != sandboxClassLoaderMap.get(namespace)) {
            classLoader = sandboxClassLoaderMap.get(namespace);
        }

        // 如果未启动则重新加载
        else {
            classLoader = new RaspClassLoader(namespace, coreJar);
            sandboxClassLoaderMap.put(namespace, classLoader);
        }

        return classLoader;
    }

    /**
     * 删除指定命名空间下的jvm-sandbox
     *
     * @param namespace 指定命名空间
     * @throws Throwable 删除失败
     */
    @SuppressWarnings("unused")
    public static synchronized void uninstall(final String namespace) throws Throwable {
        checkCaller(CLOSE_CALLER_CLASS);
        final RaspClassLoader sandboxClassLoader = sandboxClassLoaderMap.get(namespace);
        if (null == sandboxClassLoader) {
            return;
        }

        // 关闭服务器
        final Class<?> classOfProxyServer = sandboxClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);
        classOfProxyServer.getMethod("destroy")
                .invoke(classOfProxyServer.getMethod("getInstance").invoke(null));

        // 关闭SandboxClassLoader
        sandboxClassLoader.closeIfPossible();
        sandboxClassLoaderMap.remove(namespace);
    }

    /**
     * 在当前JVM安装jvm-sandbox
     *
     * @param featureMap 启动参数配置
     * @param inst       inst
     * @return 服务器IP:PORT
     */
    private static synchronized InetSocketAddress install(final Map<String, String> featureMap,
                                                          final Instrumentation inst) {

        final String namespace = getNamespace(featureMap);
        final Map<String, String> coreConfigs = toCoreConfigMap(featureMap);

        try {
            final String home = getRaspHome(featureMap);
            // 依赖的spy版本在agent加载时确定，解决业务进程不重启，而无法没有升级的问题

            final String coreVersion = getCoreVersion(featureMap);

            // 构造自定义的类加载器，尽量减少Sandbox对现有工程的侵蚀
            final ClassLoader sandboxClassLoader = loadOrDefineClassLoader(namespace, getSandboxCoreJarPath(home, coreVersion));

            // CoreConfigure类定义
            final Class<?> classOfConfigure = sandboxClassLoader.loadClass(CLASS_OF_CORE_CONFIGURE);

            // 反射生成CoreConfigure类实例
            final Object objectOfCoreConfigure = classOfConfigure.getMethod("toConfigure", Map.class)
                    .invoke(null, coreConfigs);

            // CoreServer类定义
            final Class<?> classOfProxyServer = sandboxClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);

            // 获取CoreServer单例
            final Object objectOfProxyServer = classOfProxyServer
                    .getMethod("getInstance")
                    .invoke(null);

            // CoreServer.isBind()
            final boolean isBind = (Boolean) classOfProxyServer.getMethod("isBind").invoke(objectOfProxyServer);


            // 如果未绑定,则需要绑定一个地址
            if (!isBind) {
                try {
                    classOfProxyServer
                            .getMethod("bind", classOfConfigure, Instrumentation.class)
                            .invoke(objectOfProxyServer, objectOfCoreConfigure, inst);
                } catch (Throwable t) {
                    classOfProxyServer.getMethod("destroy").invoke(objectOfProxyServer);
                    throw t;
                }

            }

            // 返回服务器绑定的地址
            return (InetSocketAddress) classOfProxyServer
                    .getMethod("getLocal")
                    .invoke(objectOfProxyServer);


        } catch (Throwable cause) {
            throw new RuntimeException("jrasp init failed.", cause);
        }

    }


    // ----------------------------------------------- 以下代码用于配置解析 -----------------------------------------------

    private static final String KEY_JRASP_HOME = "raspHome";
    private static final String KEY_LAUNCH_MODE = "mode";

    private static final String KEY_CORE_VERSION = "coreVersion";
    private static final String DEFAULT_CORE_VERSION = "1.1.5";

    private static final String KEY_NAMESPACE = "namespace";
    private static final String DEFAULT_NAMESPACE = "default";

    private static boolean isNotBlankString(final String string) {
        return null != string
                && string.length() > 0
                && !string.matches("^\\s*$");
    }

    private static boolean isBlankString(final String string) {
        return !isNotBlankString(string);
    }

    private static String getDefaultString(final String string, final String defaultString) {
        return isNotBlankString(string)
                ? string
                : defaultString;
    }

    public static Map<String, String> toFeatureMap(final String featureString) {
        final Map<String, String> featureMap = new LinkedHashMap<String, String>();

        // 不对空字符串进行解析
        if (isBlankString(featureString)) {
            return featureMap;
        }

        // KV对片段数组
        final String[] kvPairSegmentArray = featureString.split(";");
        if (kvPairSegmentArray.length <= 0) {
            return featureMap;
        }

        for (String kvPairSegmentString : kvPairSegmentArray) {
            if (isBlankString(kvPairSegmentString)) {
                continue;
            }
            // k1=v1;k2=v2;
            // 加上limit限制为2，兼容性更强，如果v1中含有"="，解析将报错
            // 例如传递如下参数 url=http://localhost:8080?index=home;
            final String[] kvSegmentArray = kvPairSegmentString.split("=", 2);
            if (kvSegmentArray.length != 2
                    || isBlankString(kvSegmentArray[0])
                    || isBlankString(kvSegmentArray[1])) {
                continue;
            }
            featureMap.put(kvSegmentArray[0], kvSegmentArray[1]);
        }

        return featureMap;
    }

    private static String getDefault(final Map<String, String> map, final String key, final String defaultValue) {
        return null != map
                && !map.isEmpty()
                ? getDefaultString(map.get(key), defaultValue)
                : defaultValue;
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static boolean isWindows() {
        return OS.contains("win");
    }

    // 获取主目录
    private static String getRaspHome(final Map<String, String> featureMap) {
        String home = getDefault(featureMap, KEY_JRASP_HOME, DEFAULT_JRASP_HOME);
        if (isWindows()) {
            Matcher m = Pattern.compile("(?i)^[/\\\\]([a-z])[/\\\\]").matcher(home);
            if (m.find()) {
                home = m.replaceFirst("$1:/");
            }
        }
        return home;
    }

    // 获取命名空间
    private static String getNamespace(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NAMESPACE, DEFAULT_NAMESPACE);
    }

    // 获取core版本
    private static String getCoreVersion(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_CORE_VERSION, DEFAULT_CORE_VERSION);
    }

    // 与agent默认参数合并后向maps中增加参数,未被合并的参数透传给core
    private static Map<String, String> toCoreConfigMap(final Map<String, String> featureMap) {
        final String raspHome = getRaspHome(featureMap);
        featureMap.put(KEY_JRASP_HOME, raspHome);
        featureMap.put(KEY_NAMESPACE, getNamespace(featureMap));
        featureMap.put(KEY_LAUNCH_MODE, LAUNCH_MODE);
        // 其他参数透传给jrasp-core
        return featureMap;
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
