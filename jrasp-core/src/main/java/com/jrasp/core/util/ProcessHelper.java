package com.jrasp.core.util;

import java.lang.management.ManagementFactory;

public class ProcessHelper {
    public static String getCurrentPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
}