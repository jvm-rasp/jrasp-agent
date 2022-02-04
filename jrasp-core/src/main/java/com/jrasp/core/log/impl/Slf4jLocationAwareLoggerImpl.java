package com.jrasp.core.log.impl;

import com.jrasp.api.log.Log;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static net.logstash.logback.marker.Markers.append;

// slf4j 高版本实现
public class Slf4jLocationAwareLoggerImpl implements Log {

    private static final String LOG_KEY = "log_id";

    private static final String HOST_NAME_KEY = "host_name";

    private static String HOST_NAME_VALUE;

    static {
        try {
            HOST_NAME_VALUE = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            HOST_NAME_VALUE = "unknown_" + RandomStringUtils.randomAlphanumeric(12);
        }
    }

    private static Marker appendHostNameAndLogId(int logId) {
        return append(LOG_KEY, logId).and(append(HOST_NAME_KEY, HOST_NAME_VALUE));
    }

    private final LocationAwareLogger logger;

    Slf4jLocationAwareLoggerImpl(LocationAwareLogger logger) {
        this.logger = logger;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(int logId, String s) {
        logger.trace(appendHostNameAndLogId(logId), s);
    }

    @Override
    public void trace(int logId, String var1, Object var2) {
        logger.trace(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void trace(int logId, String var1, Object var2, Object var3) {
        logger.trace(appendHostNameAndLogId(logId), var1, var2, var3);
    }

    @Override
    public void trace(int logId, String var1, Object... var2) {
        logger.trace(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void trace(int logId, String var1, Throwable var2) {
        logger.trace(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(int logId, String s) {
        logger.debug(appendHostNameAndLogId(logId), s);
    }

    @Override
    public void debug(int logId, String var1, Object var2) {
        logger.debug(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void debug(int logId, String var1, Object var2, Object var3) {
        logger.debug(appendHostNameAndLogId(logId), var1, var2, var3);
    }

    @Override
    public void debug(int logId, String var1, Object... var2) {
        logger.debug(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void debug(int logId, String var1, Throwable var2) {
        logger.debug(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(int logId, String s) {
        logger.info(appendHostNameAndLogId(logId), s);
    }

    @Override
    public void info(int logId, String var1, Object var2) {
        logger.info(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void info(int logId, String var1, Object var2, Object var3) {
        logger.info(appendHostNameAndLogId(logId), var1, var2, var3);
    }

    @Override
    public void info(int logId, String var1, Object... var2) {
        logger.info(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void info(int logId, String var1, Throwable var2) {
        logger.info(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(int logId, String var1) {
        logger.warn(appendHostNameAndLogId(logId), var1);
    }

    @Override
    public void warn(int logId, String var1, Object var2) {
        logger.warn(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void warn(int logId, String var1, Object... var2) {
        logger.warn(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void warn(int logId, String var1, Object var2, Object var3) {
        logger.warn(appendHostNameAndLogId(logId), var1, var2, var3);
    }

    @Override
    public void warn(int logId, String var1, Throwable var2) {
        logger.warn(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(int logId, String s, Throwable e) {
        logger.debug(appendHostNameAndLogId(logId), s, e);
    }

    @Override
    public void error(int logId, String var1) {
        logger.error(appendHostNameAndLogId(logId), var1);
    }

    @Override
    public void error(int logId, String var1, Object var2) {
        logger.error(appendHostNameAndLogId(logId), var1, var2);
    }

    @Override
    public void error(int logId, String var1, Object var2, Object var3) {
        logger.error(appendHostNameAndLogId(logId), var1, var2, var3);
    }

    @Override
    public void error(int logId, String var1, Object... var2) {
        logger.error(appendHostNameAndLogId(logId), var1, var2);
    }
}
