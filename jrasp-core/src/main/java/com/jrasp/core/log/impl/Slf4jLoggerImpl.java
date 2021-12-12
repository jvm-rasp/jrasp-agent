package com.jrasp.core.log.impl;

import com.jrasp.api.log.Log;
import org.slf4j.Logger;

// slf4j 低版本实现
public class Slf4jLoggerImpl implements Log {

    private final Logger log;

    public Slf4jLoggerImpl(Logger logger) {
        log = logger;
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