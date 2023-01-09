package com.jrasp.agent.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 自定义日志格式，来源于 tomcat-juli
 * @author jrasp
 */
public class LogFormatter extends Formatter {

    private static final String LINE_SEP = System.getProperty("line.separator");

    private static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // TODO 可以优化为threadlocal Timestamp
        // 容易内存泄漏，暂不优化
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        String time = simpleDateFormat.format(record.getMillis());
        sb.append(time);

        // Severity
        sb.append(' ');
        sb.append(record.getLevel());

        // Thread
        sb.append(' ');
        sb.append('[');
        sb.append(Thread.currentThread().getName());
        sb.append(']');

        // Source
        sb.append(' ');
        // 使用log名称
        // todo log 名称太长了，考虑截断处理
        sb.append(record.getLoggerName());
        sb.append('.');
        sb.append(record.getSourceMethodName());

        // Message
        sb.append(' ');
        sb.append(formatMessage(record));

        // New line for next record
        sb.append(LINE_SEP);

        // Stack trace
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new IndentingPrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
            sb.append(sw.getBuffer());
        }

        return sb.toString();
    }

    private static class IndentingPrintWriter extends PrintWriter {

        public IndentingPrintWriter(Writer out) {
            super(out);
        }

        @Override
        public void println(Object x) {
            super.print('\t');
            super.println(x);
        }
    }
}
