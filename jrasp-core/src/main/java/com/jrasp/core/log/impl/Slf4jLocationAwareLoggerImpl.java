package com.jrasp.core.log.impl;

import com.jrasp.api.log.Log;
import org.slf4j.spi.LocationAwareLogger;

import static net.logstash.logback.marker.Markers.append;

// slf4j 高版本实现
public class Slf4jLocationAwareLoggerImpl implements Log {

    private static final String LOG_KEY="log_id";

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
        logger.trace(append(LOG_KEY, logId), s);
    }

    @Override
    public void trace(int logId, String var1, Object var2) {
        logger.trace(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void trace(int logId, String var1, Object var2, Object var3) {
        logger.trace(append(LOG_KEY, logId), var1, var2, var3);
    }

    @Override
    public void trace(int logId, String var1, Object... var2) {
        logger.trace(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void trace(int logId, String var1, Throwable var2) {
        logger.trace(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(int logId, String s) {
        logger.debug(append(LOG_KEY, logId), s);
    }

    @Override
    public void debug(int logId, String var1, Object var2) {
        logger.debug(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void debug(int logId, String var1, Object var2, Object var3) {
        logger.debug(append(LOG_KEY, logId), var1, var2, var3);
    }

    @Override
    public void debug(int logId, String var1, Object... var2) {
        logger.debug(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void debug(int logId, String var1, Throwable var2) {
        logger.debug(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(int logId, String s) {
        logger.info(append(LOG_KEY, logId), s);
    }

    @Override
    public void info(int logId, String var1, Object var2) {
        logger.info(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void info(int logId, String var1, Object var2, Object var3) {
        logger.info(append(LOG_KEY, logId), var1, var2, var3);
    }

    @Override
    public void info(int logId, String var1, Object... var2) {
        logger.info(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void info(int logId, String var1, Throwable var2) {
        logger.info(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(int logId, String var1) {
        logger.warn(append(LOG_KEY, logId), var1);
    }

    @Override
    public void warn(int logId, String var1, Object var2) {
        logger.warn(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void warn(int logId, String var1, Object... var2) {
        logger.warn(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void warn(int logId, String var1, Object var2, Object var3) {
        logger.warn(append(LOG_KEY, logId), var1, var2, var3);
    }

    @Override
    public void warn(int logId, String var1, Throwable var2) {
        logger.warn(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(int logId, String s, Throwable e) {
        logger.debug(append(LOG_KEY, logId), s, e);
    }

    @Override
    public void error(int logId, String var1) {
        logger.error(append(LOG_KEY, logId), var1);
    }

    @Override
    public void error(int logId, String var1, Object var2) {
        logger.error(append(LOG_KEY, logId), var1, var2);
    }

    @Override
    public void error(int logId, String var1, Object var2, Object var3) {
        logger.error(append(LOG_KEY, logId), var1, var2, var3);
    }

    @Override
    public void error(int logId, String var1, Object... var2) {
        logger.error(append(LOG_KEY, logId), var1, var2);
    }
}
