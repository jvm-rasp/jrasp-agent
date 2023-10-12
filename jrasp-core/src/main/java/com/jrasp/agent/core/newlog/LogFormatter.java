package com.jrasp.agent.core.newlog;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

/**
 * 输出json日志便于解析
 *
 * @author jrasp
 * @since 2023/5/27
 */
public class LogFormatter {

    private static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private static String host = "";
    private static int pid = 0;

    public static String format(Level level, int logId, String message) {
        return format(level, logId, message, null);
    }

    public static String format(Level level, int logId, String message, Throwable t) {
        JSONObject json = new JSONObject();
        json.put("topic", "jrasp-agent"); // topic: jrasp-agent、jrasp-daemon
        json.put("level", level);
        json.put("logId", logId);
        json.put("ts", getTimestamp());
        json.put("thread", getCurrentThreadName());
        json.put("pid", getProcessId());
        json.put("hostName", getHostName());
        json.put("msg", message);
        json.put("stackTrace", t == null ? "" : getStackTrace(t));
        return json.toString();
    }

    private static String getTimestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        return simpleDateFormat.format(System.currentTimeMillis());
    }

    private static String getCurrentThreadName() {
        return Thread.currentThread().getName();
    }

    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.close();
        String stackTrace = sw.getBuffer().toString();
        return stackTrace;
    }

    private static String getHostName() {
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

    private static int getProcessId() {
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