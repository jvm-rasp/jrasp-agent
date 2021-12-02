package com.jrasp.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// RASPAgent启动器
public class AgentLauncher {

    // rasp安装目录配置的KEY和默认值
    private static final String KEY_JRASP_HOME = "raspHome";
    private static final String DEFAULT_JRASP_HOME = new File(AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParent();

    // 命名空间配置到的KEY和默认值
    private static final String KEY_NAMESPACE = "namespace";
    private static final String DEFAULT_NAMESPACE = "jrasp";

    // Ip值配置的KEY和默认值
    private static final String KEY_SERVER_IP = "serverIp";
    private static final String DEFAULT_IP = "0.0.0.0";

    // Port值配置的KEY和默认值
    private static final String KEY_SERVER_PORT = "serverPort";
    private static final String DEFAULT_PORT = "0";

    // token配置的KEY和默认值
    private static final String KEY_TOKEN = "token";
    private static final String DEFAULT_TOKEN = "";

    // agent方式配置的KEY和默认值
    private static final String KEY_LAUNCH_MODE = "mode";
    private static String LAUNCH_MODE;  // 启动模式
    private static final String LAUNCH_MODE_AGENT = "agent"; // agent方式加载
    private static final String LAUNCH_MODE_ATTACH = "attach"; // 启动模式: attach方式加载

    private static String getRaspCoreJarPath(String raspHome) {
        return raspHome + File.separatorChar + "lib" + File.separator + "jrasp-core.jar";
    }

    private static String getRaspSpyJarPath(String raspHome) {
        return raspHome + File.separatorChar + "lib" + File.separator + "jrasp-spy.jar";
    }

    // 全局持有ClassLoader用于隔离rasp实现
    private static volatile Map<String/*NAMESPACE*/, RaspClassLoader> raspClassLoaderMap
            = new ConcurrentHashMap<String, RaspClassLoader>();

    private static final String CLASS_OF_CORE_CONFIGURE = "com.jrasp.core.CoreConfigure";

    private static final String CLASS_OF_PROXY_CORE_SERVER = "com.jrasp.core.server.ProxyCoreServer";

    // 启动加载
    public static void premain(String featureString, Instrumentation inst) {
        LAUNCH_MODE = LAUNCH_MODE_AGENT;
        install(toFeatureMap(featureString), inst);
    }

    // 动态加载
    public static void agentmain(String featureString, Instrumentation inst) {
        LAUNCH_MODE = LAUNCH_MODE_ATTACH;
        install(toFeatureMap(featureString), inst);
    }

    private static synchronized ClassLoader loadOrDefineClassLoader(final String namespace,
                                                                    final String coreJar) throws Throwable {
        final RaspClassLoader classLoader;
        // 如果已经被启动则返回之前启动的ClassLoader
        if (raspClassLoaderMap.containsKey(namespace)
                && null != raspClassLoaderMap.get(namespace)) {
            classLoader = raspClassLoaderMap.get(namespace);
        }
        // 如果未启动则重新加载
        else {
            classLoader = new RaspClassLoader(namespace, coreJar);
            raspClassLoaderMap.put(namespace, classLoader);
        }
        return classLoader;
    }

    // 删除指定命名空间下的rasp;被反射调用
    public static synchronized void uninstall(final String namespace) throws Throwable {
        final RaspClassLoader raspClassLoader = raspClassLoaderMap.get(namespace);
        if (null == raspClassLoader) {
            return;
        }
        // 关闭服务器
        final Class<?> classOfProxyServer = raspClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);
        classOfProxyServer.getMethod("destroy")
                .invoke(classOfProxyServer.getMethod("getInstance").invoke(null));
        // 关闭raspClassLoader
        raspClassLoader.closeIfPossible();
        raspClassLoaderMap.remove(namespace);
    }

    // 在当前JVM安装rasp
    private static synchronized InetSocketAddress install(final Map<String, String> featureMap,
                                                          final Instrumentation inst) {
        final String namespace = getNamespace(featureMap);
        Map<String, String> coreConfigMap = toCoreConfigMap(featureMap);
        try {
            final String home = getRaspHome(featureMap);
            // 将Spy注入到BootstrapClassLoader
            inst.appendToBootstrapClassLoaderSearch(new JarFile(new File(
                    getRaspSpyJarPath(home)
            )));
            // 构造自定义的类加载器，尽量减少Rasp对现有工程的侵蚀
            final ClassLoader raspClassLoader = loadOrDefineClassLoader(
                    namespace,
                    getRaspCoreJarPath(home)
            );
            // CoreConfigure类定义
            final Class<?> classOfConfigure = raspClassLoader.loadClass(CLASS_OF_CORE_CONFIGURE);
            // 反序列化成CoreConfigure类实例
            final Object objectOfCoreConfigure = classOfConfigure.getMethod("toConfigure", Map.class)
                    .invoke(null, coreConfigMap);
            // CoreServer类定义
            final Class<?> classOfProxyServer = raspClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);
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
            throw new RuntimeException("rasp attach failed.", cause);
        }
    }

    // ----------------------------------------------- 配置值和系统默认值合并 ----------------------------------------------
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

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static boolean isWindows() {
        return OS.contains("win");
    }

    // 获取命名空间
    private static String getNamespace(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NAMESPACE, DEFAULT_NAMESPACE);
    }

    // 获取TOKEN todo 优化
    private static String getToken(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_TOKEN, DEFAULT_TOKEN);
    }

    // 获取ip
    private static String getServerIp(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_SERVER_IP, DEFAULT_IP);
    }

    // 获取port
    private static String getServerPort(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_SERVER_PORT, DEFAULT_PORT);
    }

    // 与agent默认参数合并后向maps中增加参数,未被合并的参数透传给core
    private static Map<String, String> toCoreConfigMap(final Map<String, String> featureMap) {
        final String raspHome = getRaspHome(featureMap);
        featureMap.put(KEY_JRASP_HOME, raspHome);
        featureMap.put(KEY_NAMESPACE, getNamespace(featureMap));
        featureMap.put(KEY_LAUNCH_MODE, LAUNCH_MODE);
        featureMap.put(KEY_SERVER_IP, getServerIp(featureMap));
        featureMap.put(KEY_SERVER_PORT, getServerPort(featureMap));
        // 其他参数透传
        return featureMap;
    }

    // ----------------------------------------------- 工具方法 ---------------------------------------------------------
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

    private static Map<String, String> toFeatureMap(final String featureString) {
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
            final String[] kvSegmentArray = kvPairSegmentString.split("=");
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
                : defaultValue; // map==null
    }

}
