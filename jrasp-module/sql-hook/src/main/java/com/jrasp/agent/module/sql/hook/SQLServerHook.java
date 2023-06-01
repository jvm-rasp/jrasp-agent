package com.jrasp.agent.module.sql.hook;

import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.Reflection;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class SQLServerHook {

    private final static String TYPE = "sqlserver";
    private AlgorithmManager algorithmManager;
    private ModuleEventWatcher moduleEventWatcher;
    private ThreadLocal<Context> context;
    public SQLServerHook(ModuleEventWatcher moduleEventWatcher, AlgorithmManager algorithmManager, ThreadLocal<Context> context) {
        this.context = context;
        this.algorithmManager = algorithmManager;
        this.moduleEventWatcher = moduleEventWatcher;
        hookExecute();
        hookConnection();
        hookPreparedStatementException();
        hookSlowQuery();
    }

    private void hookExecute() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/microsoft/sqlserver/jdbc/SQLServerStatement")
                        .onMethod(
                                new String[]{
                                        "execute(Ljava/lang/String;)Z",
                                        "execute(Ljava/lang/String;I)Z",
                                        "execute(Ljava/lang/String;[I)Z",
                                        "execute(Ljava/lang/String;[Ljava/lang/String;)Z",
                                        "executeUpdate(Ljava/lang/String;I)I",
                                        "executeUpdate(Ljava/lang/String;[I)I",
                                        "executeUpdate(Ljava/lang/String;[Ljava/lang/String;)I",
                                        "executeUpdate(Ljava/lang/String;)I",
                                        "executeLargeUpdate(Ljava/lang/String;)J",
                                        "executeLargeUpdate(Ljava/lang/String;I)J",
                                        "executeLargeUpdate(Ljava/lang/String;[I)J",
                                        "executeLargeUpdate(Ljava/lang/String;[Ljava/lang/String;)J",
                                        "addBatch(Ljava/lang/String;)V",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                },
                                new SqlExecuteAdviceListener()
                        ))
                .onClass(new ClassMatcher("com/microsoft/sqlserver/jdbc/SQLServerConnection")
                        .onMethod(
                                new String[]{
                                        "prepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;IILcom/microsoft/sqlserver/jdbc/SQLServerStatementColumnEncryptionSetting;)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;IIILcom/microsoft/sqlserver/jdbc/SQLServerStatementColumnEncryptionSetting;)Ljava/sql/PreparedStatement;",
                                        "prepareCall(Ljava/lang/String;II)Ljava/sql/CallableStatement;",
                                        "prepareCall(Ljava/lang/String;IILcom/microsoft/sqlserver/jdbc/SQLServerStatementColumnEncryptionSetting;)Ljava/sql/CallableStatement;",
                                        "prepareCall(Ljava/lang/String;IIILcom/microsoft/sqlserver/jdbc/SQLServerStatementColumnEncryptionSetting;)Ljava/sql/CallableStatement;",
                                },
                                new SqlExecuteAdviceListener()
                        ))
                .build();
    }

    private void hookPreparedStatementException() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement")
                        .onMethod(
                                new String[]{
                                        "execute()Z",
                                        "execute(Ljava/lang/String;)Z",
                                        "executeUpdate()I",
                                        "executeUpdate(Ljava/lang/String;)I",
                                        "executeQuery()Ljava/sql/ResultSet;",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "executeBatch()[I",
                                        "executeBatchInternal()[J"
                                },
                                new SqlStatementAdviceListener()
                        ))
                .build();
    }

    private void hookConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/microsoft/sqlserver/jdbc/SQLServerDriver")
                        .onMethod("connect(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;",
                                new SqlConnectionAdviceListener()))
                .build();
    }

    private void hookSlowQuery() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/microsoft/sqlserver/jdbc/SQLServerResultSet")
                        .onMethod("next()Z", new SqlResultSetAdviceListener()))
                .build();
    }

    private class SqlExecuteAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            String query = (String) advice.getParameterArray()[0];
            if (query != null && !query.isEmpty()) {
                params.put("server", "sqlserver");
                params.put("type", "injection");
                params.put("sql", query);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            Throwable exception = advice.getThrowable();
            if (exception instanceof java.sql.SQLException) {
                String query = (String) advice.getParameterArray()[0];
                if (query != null && !query.isEmpty()) {
                    params.put("server", "sqlserver");
                    params.put("type", "exception");
                    params.put("exception", exception);
                    params.put("sql", query);
                    algorithmManager.doCheck(TYPE, context.get(), params);
                }
            }
        }
    }

    private class SqlStatementAdviceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            String query = null;
            Object self = advice.getTarget();
            try {
                query = (String) Reflection.getSuperField(self, "preparedSQL");
            } catch (NullPointerException ignored) {
            } catch (NoSuchFieldException e) {
                query = (String) Reflection.getField(self, "preparedSQL");
            }
            if (query != null && !query.isEmpty()) {
                params.put("server", "sqlserver");
                params.put("type", "injection");
                params.put("sql", query);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            String query = "";
            Object that = advice.getTarget();
            try {
                query = (String) Reflection.getSuperField(that, "preparedSQL");
            } catch (NullPointerException ignored) {
            } catch (NoSuchFieldException e) {
                query = (String) Reflection.getField(that, "preparedSQL");
            }
            Throwable exception = advice.getThrowable();
            if (exception instanceof java.sql.SQLException) {
                params.put("server", "sqlserver");
                params.put("type", "exception");
                params.put("sql", query);
                params.put("exception", exception);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }
    }

    private class SqlConnectionAdviceListener extends AdviceListener {
        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            Throwable exception = advice.getThrowable();
            if (exception instanceof java.sql.SQLException) {
                params.put("server", "sqlserver");
                params.put("type", "exception");
                params.put("exception", exception);
                params.put("jdbc_url", advice.getParameterArray()[0]);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }
    }

    private class SqlResultSetAdviceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            ResultSet sqlResultSet = (ResultSet) advice.getTarget();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("server", "sqlserver");
            params.put("type", "slowQuery");
            params.put("query_count", sqlResultSet.getRow());
            algorithmManager.doCheck(TYPE, context.get(), params);
        }
    }
}
