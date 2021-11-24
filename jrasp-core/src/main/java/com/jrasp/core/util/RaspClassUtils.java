package com.jrasp.core.util;

public class RaspClassUtils {

    private static final String RASP_FAMILY_CLASS_RES_PREFIX = "com/jrasp";
    private static final String RASP_FAMILY_CLASS_RES_QATEST_PREFIX = "com/jrasp/qatest";

    public static boolean isComeFromRaspFamily(final String internalClassName, final ClassLoader loader) {

        if (null != internalClassName
                && isRaspPrefix(internalClassName)) {
            return true;
        }

        if (null != loader
                && isRaspPrefix(normalizeClass(loader.getClass().getName()))) {
            return true;
        }

        return false;

    }

    private static String normalizeClass(String className) {
        return className.replace(".", "/");
    }

    private static boolean isRaspPrefix(String internalClassName) {
        return internalClassName.startsWith(RASP_FAMILY_CLASS_RES_PREFIX)
                && !isQaTestPrefix(internalClassName);
    }

    private static boolean isQaTestPrefix(String internalClassName) {
        return internalClassName.startsWith(RASP_FAMILY_CLASS_RES_QATEST_PREFIX);
    }

}
