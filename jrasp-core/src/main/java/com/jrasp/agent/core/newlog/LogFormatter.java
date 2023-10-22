package com.jrasp.agent.core.newlog;

import com.jrasp.agent.core.util.ProcessHelper;
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

    public static String format(Level level, int logId, String message, String processId) {
        return format(level, logId, message, processId, null);
    }

    public static String format(Level level, int logId, String message, String processId, Throwable t) {
        JSONObject json = new JSONObject();
        json.put("topic", "jrasp-agent"); // topic: jrasp-agent、jrasp-daemon
        json.put("level", level);
        json.put("logId", logId);
        json.put("ts", getTimestamp());
        json.put("processId", processId);
        json.put("thread", getCurrentThreadName());
        json.put("pid", ProcessHelper.getProcessId());
        json.put("hostName", ProcessHelper.getHostName());
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
        return sw.getBuffer().toString();
    }
}