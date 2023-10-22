package com.jrasp.agent.core.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author jrasp
 */
public class ProcessHelper {

    private static String host = "";
    private static int pid = 0;

    public static String getCurrentPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    public static String getHostName() {
        if ("".equals(host)) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                host = addr.getHostName();
            } catch (UnknownHostException e) {
                host = "unknown";
            }
        }
        return host;
    }

    public static int getProcessId() {
        if (pid == 0) {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            int atIndex = jvmName.indexOf('@');
            if (atIndex > 0) {
                pid = Integer.parseInt(jvmName.substring(0, atIndex));
            } else {
                pid = -1;
            }
        }
        return pid;
    }
}
