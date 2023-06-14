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

public class OracleHook {

    private final static String TYPE = "oracle";
    private AlgorithmManager algorithmManager;
    private ModuleEventWatcher moduleEventWatcher;
    private ThreadLocal<Context> context;

    public OracleHook(ModuleEventWatcher moduleEventWatcher, AlgorithmManager algorithmManager, ThreadLocal<Context> context) {
        this.context = context;
        this.algorithmManager = algorithmManager;
        this.moduleEventWatcher = moduleEventWatcher;
        hookExecute();
        hookConnection();
        hookPreparedStatementException();
    }

    private void hookExecute() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("oracle/jdbc/driver/OracleStatement")
                        .onMethod(
                                new String[]{
                                        "execute(Ljava/lang/String;)Z",
                                        "execute(Ljava/lang/String;I)Z",
                                        "execute(Ljava/lang/String;[I)Z",
                                        "execute(Ljava/lang/String;[Ljava/lang/String;)Z",
                                        "executeUpdate(Ljava/lang/String;)I",
                                        "executeUpdate(Ljava/lang/String;I)I",
                                        "executeUpdate(Ljava/lang/String;[I)I",
                                        "executeUpdate(Ljava/lang/String;[Ljava/lang/String;)I",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "executeLargeUpdate(Ljava/lang/String;)J",
                                        "executeLargeUpdate(Ljava/lang/String;I)J",
                                        "executeLargeUpdate(Ljava/lang/String;[I)J",
                                        "executeLargeUpdate(Ljava/lang/String;[Ljava/lang/String;)J",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "addBatch(Ljava/lang/String;)V"
                                }
                                , new SqlExecuteAdviceListener()
                        ))
                .onClass(new ClassMatcher("oracle/jdbc/driver/PhysicalConnection")
                        .onMethod(
                                new String[]{
                                        "prepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "prepareCall(Ljava/lang/String;II)Ljava/sql/CallableStatement;",
                                }
                                , new SqlExecuteAdviceListener()
                        ))
                .build();
    }

    private void hookPreparedStatementException() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("oracle/jdbc/driver/OraclePreparedStatement")
                        .onMethod(
                                new String[]{
                                        "execute(Ljava/lang/String;)Z",
                                        "execute(Ljava/lang/String;I)Z",
                                        "execute(Ljava/lang/String;[I)Z",
                                        "execute(Ljava/lang/String;[Ljava/lang/String;)Z",
                                        "executeUpdate(Ljava/lang/String;)I",
                                        "executeUpdate(Ljava/lang/String;I)I",
                                        "executeUpdate(Ljava/lang/String;[I)I",
                                        "executeUpdate(Ljava/lang/String;[Ljava/lang/String;)I",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "executeBatch()[I",
                                        "executeBatchInternal()[J"
                                }
                                , new SqlStatementAdviceListener()
                        ))
                .build();
    }

    private void hookConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("oracle/jdbc/driver/OracleDriver")
                        .onMethod("connect(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;",
                                new SqlConnectionAdviceListener()))
                .build();
    }

    private class SqlExecuteAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            String stmt = (String) advice.getParameterArray()[0];
            if (stmt != null) {
                params.put("server", "oracle");
                params.put("type", "injection");
                params.put("sql", stmt);
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
                if (query != null) {
                    params.put("server", "oracle");
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

            Object self = advice.getTarget();
            Object sqlObject = Reflection.getSuperField(self, "sqlObject");
            String query = Reflection.invokeStringMethod(sqlObject, "toString", new Class[]{});
            if (query != null && !query.isEmpty()) {
                params.put("server", "oracle");
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
            try {
                Object that = advice.getTarget();
                Object thisSqlObject = Reflection.getSuperField(that, "sqlObject");
                query = Reflection.invokeStringMethod(thisSqlObject, "getOriginalSql", new Class[]{});
            } catch (NullPointerException ignored) {
            }
            Throwable exception = advice.getThrowable();
            if (exception instanceof java.sql.SQLException) {
                params.put("server", "oracle");
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
                params.put("server", "oracle");
                params.put("type", "exception");
                params.put("exception", exception);
                params.put("jdbc_url", advice.getParameterArray()[0]);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }
    }
}
