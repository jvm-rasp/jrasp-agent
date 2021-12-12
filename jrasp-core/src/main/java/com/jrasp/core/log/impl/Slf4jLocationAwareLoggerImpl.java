package com.jrasp.core.log.impl;

import com.jrasp.api.log.Log;
import org.slf4j.spi.LocationAwareLogger;

// slf4j 高版本实现
public class Slf4jLocationAwareLoggerImpl implements Log {

    private final LocationAwareLogger logger;

    Slf4jLocationAwareLoggerImpl(LocationAwareLogger logger) {
        this.logger = logger;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public void debug(String var1, Object var2) {
        logger.debug(var1, var2);
    }

    @Override
    public void debug(String var1, Object var2, Object var3) {
        logger.debug(var1, var2, var3);
    }

    @Override
    public void debug(String var1, Object... var2) {
        logger.debug(var1, var2);
    }

    @Override
    public void debug(String var1, Throwable var2) {
        logger.debug(var1, var2);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void error(String s, Throwable e) {
        logger.debug(s, e);
    }

    @Override
    public void error(String var1) {
        logger.error(var1);
    }

    @Override
    public void error(String var1, Object var2) {
        logger.error(var1, var2);
    }

    @Override
    public void error(String var1, Object var2, Object var3) {
        logger.error(var1, var2, var3);
    }

    @Override
    public void error(String var1, Object... var2) {
        logger.error(var1, var2);
    }


    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(s);
    }

    @Override
    public void trace(String var1, Object var2) {
        logger.trace(var1, var2);
    }

    @Override
    public void trace(String var1, Object var2, Object var3) {
        logger.trace(var1, var2, var3);
    }

    @Override
    public void trace(String var1, Object... var2) {
        logger.trace(var1, var2);
    }

    @Override
    public void trace(String var1, Throwable var2) {
        logger.trace(var1, var2);
    }

    @Override
    public void warn(String var1) {
        logger.warn(var1);
    }

    @Override
    public void warn(String var1, Object var2) {
        logger.warn(var1, var2);
    }

    @Override
    public void warn(String var1, Object... var2) {
        logger.warn(var1, var2);
    }

    @Override
    public void warn(String var1, Object var2, Object var3) {
        logger.warn(var1, var2, var3);
    }

    @Override
    public void warn(String var1, Throwable var2) {
        logger.warn(var1, var2);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String var1, Object var2) {
        logger.info(var1, var2);
    }

    @Override
    public void info(String var1, Object var2, Object var3) {
        logger.info(var1, var2, var3);
    }

    @Override
    public void info(String var1, Object... var2) {
        logger.info(var1, var2);
    }

    @Override
    public void info(String var1, Throwable var2) {
        logger.info(var1, var2);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }
}
