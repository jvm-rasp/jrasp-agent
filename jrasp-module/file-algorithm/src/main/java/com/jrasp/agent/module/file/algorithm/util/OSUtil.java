package com.jrasp.agent.module.file.algorithm.util;

public class OSUtil {
    public static boolean isWindows() {
        return (System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("windows"));
    }

    public static boolean isLinux() {
        return (System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("linux"));
    }

    public static boolean isMacOS() {
        return (System.getProperty("os.name") != null && System.getProperty("os.name").toLowerCase().contains("mac os x"));
    }
}
