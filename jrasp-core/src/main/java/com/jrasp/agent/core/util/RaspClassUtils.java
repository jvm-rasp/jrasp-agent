package com.jrasp.agent.core.util;

public class RaspClassUtils {

    private static final String RASP_FAMILY_CLASS_RES_PREFIX = "com/jrasp/agent/";

    public static boolean isComeFromRaspFamily(final String internalClassName, final ClassLoader loader) {
        // 类名是com.jrasp.agent开头
        if (null != internalClassName && isRaspPrefix(internalClassName)) {
            return true;
        }
        // 类被com.jrasp.agent开头的ClassLoader所加载
        if (null != loader && isRaspPrefix(normalizeClass(loader.getClass().getName()))) {
            return true;
        }
        return false;

    }

    /**
     * 标准化类名
     * <p>
     * 入参：com.jrasp.agent
     * 返回：com/jrasp/agent
     * </p>
     *
     * @param className 类名
     * @return 标准化类名
     */
    private static String normalizeClass(String className) {
        return className.replace('.', '/');
    }

    /**
     * 是否是RASP自身的类
     * <p>
     * 需要注意internalClassName的格式形如: com/jrasp/agent
     *
     * @param internalClassName 类资源名
     * @return true / false
     */
    private static boolean isRaspPrefix(String internalClassName) {
        return internalClassName.startsWith(RASP_FAMILY_CLASS_RES_PREFIX);
    }

    /**
     * 提前加载某些必要的类
     */
    public static void doEarlyLoadSandboxClass(String... classNames) {
        for (String className : classNames) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

}
