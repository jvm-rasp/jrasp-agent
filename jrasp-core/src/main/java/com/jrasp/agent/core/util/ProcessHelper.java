package com.jrasp.agent.core.util;

import java.lang.management.ManagementFactory;

/**
 * @author jrasp
 */
public class ProcessHelper {
    public static String getCurrentPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
}
