package com.jrasp.api.log;

public interface Log {

    String getName();

    boolean isTraceEnabled();

    void trace(int logId, String var1);

    void trace(int logId, String var1, Object var2);

    void trace(int logId, String var1, Object var2, Object var3);

    void trace(int logId, String var1, Object... var2);

    void trace(int logId, String var1, Throwable var2);

    boolean isDebugEnabled();

    void debug(int logId, String var1);

    void debug(int logId, String var1, Object var2);

    void debug(int logId, String var1, Object var2, Object var3);

    void debug(int logId, String var1, Object... var2);

    void debug(int logId, String var1, Throwable var2);

    boolean isInfoEnabled();

    void info(int logId, String var1);

    void info(int logId, String var1, Object var2);

    void info(int logId, String var1, Object var2, Object var3);

    void info(int logId, String var1, Object... var2);

    void info(int logId, String var1, Throwable var2);

    boolean isWarnEnabled();

    void warn(int logId, String var1);

    void warn(int logId, String var1, Object var2);

    void warn(int logId, String var1, Object... var2);

    void warn(int logId, String var1, Object var2, Object var3);

    void warn(int logId, String var1, Throwable var2);

    boolean isErrorEnabled();

    void error(int logId, String var1);

    void error(int logId, String var1, Object var2);

    void error(int logId, String var1, Object var2, Object var3);

    void error(int logId, String var1, Object... var2);

    void error(int logId, String var1, Throwable var2);

}
