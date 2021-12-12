package com.jrasp.core.log;

import com.jrasp.api.log.Log;
import com.jrasp.core.log.impl.Slf4jImpl;

import java.lang.reflect.Constructor;

public final class LogFactory {

    private static Constructor<? extends Log> logConstructor;

    static {
        tryImplementation(new Runnable() {
            @Override
            public void run() {
                useSlf4jLogging();
            }
        });
    }

    private LogFactory() {
        // disable construction
    }

    public static Log getLog(Class<?> clazz) {
        return getLog(clazz.getName());
    }

    public static Log getLog(String logger) {
        try {
            return logConstructor.newInstance(logger);
        } catch (Throwable t) {
            throw new LogException("Error creating logger for logger " + logger + ".  Cause: " + t, t);
        }
    }

    public static synchronized void useSlf4jLogging() {
        setImplementation(Slf4jImpl.class);
    }

    private static void tryImplementation(Runnable runnable) {
        if (logConstructor == null) {
            try {
                runnable.run();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    private static void setImplementation(Class<? extends Log> implClass) {
        try {
            Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
            Log log = candidate.newInstance(LogFactory.class.getName());
            if (log.isDebugEnabled()) {
                // todo 这个日志打印到用户进程日志了，需要找下原因
                // 10:48:55.483 [Attach Listener] DEBUG com.jrasp.core.log.LogFactory - Logging initialized using 'class com.jrasp.core.log.impl.Slf4jImpl' adapter.
                log.debug("Logging initialized using '" + implClass + "' adapter.");
            }
            logConstructor = candidate;
        } catch (Throwable t) {
            throw new LogException("Error setting Log implementation.  Cause: " + t, t);
        }
    }

}