package com.jrasp.core.log.impl;

import com.jrasp.api.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

// 2种实现的包装器
public class Slf4jImpl implements Log {

    private Log log;

    // 被反射初始化
    public Slf4jImpl(String clazz) {
        Logger logger = LoggerFactory.getLogger(clazz);

        if (logger instanceof LocationAwareLogger) {
            try {
                // check for slf4j >= 1.6 method signature
                logger.getClass().getMethod("log", Marker.class, String.class, int.class, String.class, Object[].class, Throwable.class);
                log = new Slf4jLocationAwareLoggerImpl((LocationAwareLogger) logger);
                return;
            } catch (Exception e) {
                // fail-back to Slf4jLoggerImpl
            }
        }

        // Logger is not LocationAwareLogger or slf4j version < 1.6
        log = new Slf4jLoggerImpl(logger);
    }


    @Override
    public String getName() {
        return log.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String var1) {
        log.trace(var1);
    }

    @Override
    public void trace(String var1, Object var2) {
        log.trace(var1, var2);
    }

    @Override
    public void trace(String var1, Object var2, Object var3) {
        log.trace(var1, var2, var3);
    }

    @Override
    public void trace(String var1, Object... var2) {
        log.trace(var1, var2);
    }

    @Override
    public void trace(String var1, Throwable var2) {
        log.trace(var1, var2);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String var1) {
        log.debug(var1);
    }

    @Override
    public void debug(String var1, Object var2) {
        log.debug(var1, var2);
    }

    @Override
    public void debug(String var1, Object var2, Object var3) {
        log.debug(var1, var2, var3);
    }

    @Override
    public void debug(String var1, Object... var2) {
        log.debug(var1, var2);
    }

    @Override
    public void debug(String var1, Throwable var2) {
        log.debug(var1, var2);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(String var1) {
        log.info(var1);
    }

    @Override
    public void info(String var1, Object var2) {
        log.info(var1, var2);
    }

    @Override
    public void info(String var1, Object var2, Object var3) {
        log.info(var1, var2, var3);
    }

    @Override
    public void info(String var1, Object... var2) {
        log.info(var1, var2);
    }

    @Override
    public void info(String var1, Throwable var2) {
        log.info(var1, var2);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(String var1) {
        log.warn(var1);
    }

    @Override
    public void warn(String var1, Object var2) {
        log.warn(var1, var2);
    }

    @Override
    public void warn(String var1, Object... var2) {
        log.warn(var1, var2);
    }

    @Override
    public void warn(String var1, Object var2, Object var3) {
        log.warn(var1, var2, var3);
    }

    @Override
    public void warn(String var1, Throwable var2) {
        log.warn(var1, var2);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(String var1) {
        log.error(var1);
    }

    @Override
    public void error(String var1, Object var2) {
        log.error(var1, var2);
    }

    @Override
    public void error(String var1, Object var2, Object var3) {
        log.error(var1, var2, var3);
    }

    @Override
    public void error(String var1, Object... var2) {
        log.error(var1, var2);
    }

    @Override
    public void error(String var1, Throwable var2) {
        log.error(var1, var2);
    }

}