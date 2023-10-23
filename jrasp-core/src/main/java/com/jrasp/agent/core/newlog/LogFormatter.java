package com.jrasp.agent.core.newlog;

import com.jrasp.agent.core.util.ProcessHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

/**
 * 输出json日志便于解析
 *
 * @author jrasp
 * @since 2023/5/27
 */
public class LogFormatter {

    private static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static String format(Level level, int logId, String message, String processId, Throwable t) {
        LogRecord logRecord = new LogRecord("jrasp-agent", level, logId, getTimestamp(), processId, getCurrentThreadName(),
                ProcessHelper.getCurrentPid(), ProcessHelper.getHostName(), message);
        logRecord.setStackTrace(t == null ? "" : getStackTrace(t));
        return logRecord.toString();
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