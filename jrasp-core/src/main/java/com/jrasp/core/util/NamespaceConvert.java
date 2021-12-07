package com.jrasp.core.util;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class NamespaceConvert extends ClassicConverter {

    private static volatile String namespace;
    private static String pid = ProcessHelper.getCurrentPID();

    @Override
    public String convert(ILoggingEvent event) {
        return null == namespace
                ? "NULL"
                : namespace;
    }

    public static void initNamespaceConvert(final String namespace) {
        NamespaceConvert.namespace = namespace;
        PatternLayout.defaultConverterMap.put("JRASP_NAMESPACE", NamespaceConvert.class.getName());
        PatternLayout.defaultConverterMap.put("PID", pid);
    }

}
