package com.jrasp.agent.module.sql.hook;

import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.Reflection;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class MySQLHook {

    private final static String TYPE = "mysql";
    private AlgorithmManager algorithmManager;
    private ModuleEventWatcher moduleEventWatcher;
    private ThreadLocal<Context> context;

    public MySQLHook(ModuleEventWatcher moduleEventWatcher, AlgorithmManager algorithmManager, ThreadLocal<Context> context) {
        this.context = context;
        this.algorithmManager = algorithmManager;
        this.moduleEventWatcher = moduleEventWatcher;
        hookExecute();
        hookConnection();
        hookPreparedStatementException();
    }

    private void hookExecute() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/mysql/jdbc/StatementImpl")
                        .onMethod(
                                new String[]{
                                        "executeInternal(Ljava/lang/String;Z)Z",
                                        "executeUpdateInternal(Ljava/lang/String;ZZ)J",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "addBatch(Ljava/lang/String;)V",
                                }, new SqlExecuteAdviceListener()
                        ))
                .onClass(new ClassMatcher("com/mysql/cj/jdbc/StatementImpl")
                        .onMethod(
                                new String[]{
                                        "executeInternal(Ljava/lang/String;Z)Z",
                                        "executeUpdateInternal(Ljava/lang/String;ZZ)J",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "addBatch(Ljava/lang/String;)V",
                                }, new SqlExecuteAdviceListener()
                        ))
                .onClass(new ClassMatcher("com/mysql/jdbc/ConnectionImpl")
                        .onMethod(
                                new String[]{
                                        "prepareStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                        "prepareCall(Ljava/lang/String;II)Ljava/sql/CallableStatement;",
                                        "clientPrepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;"
                                }, new SqlExecuteAdviceListener()
                        ))
                .onClass(new ClassMatcher("com/mysql/cj/jdbc/ConnectionImpl")
                        .onMethod(
                                new String[]{
                                        "prepareStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
                                        "prepareStatement(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                        "prepareCall(Ljava/lang/String;II)Ljava/sql/CallableStatement;",
                                        "clientPrepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;I)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;II)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;III)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;[I)Ljava/sql/PreparedStatement;",
                                        "serverPrepareStatement(Ljava/lang/String;[Ljava/lang/String;)Ljava/sql/PreparedStatement;"
                                }, new SqlExecuteAdviceListener()
                        ))
                .build();
    }

    private void hookConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/mysql/jdbc/NonRegisteringDriver")
                        .onMethod("connect(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;", new SqlConnectionAdviceListener()))
                .onClass(new ClassMatcher("com/mysql/cj/jdbc/NonRegisteringDriver")
                        .onMethod("connect(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;", new SqlConnectionAdviceListener()))
                .build();
    }

    private void hookPreparedStatementException() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("com/mysql/jdbc/PreparedStatement")
                        .onMethod(
                                new String[]{
                                        "execute()Z",
                                        "execute(Ljava/lang/String;)Z",
                                        "execute(Ljava/lang/String;I)Z",
                                        "execute(Ljava/lang/String;[I)Z",
                                        "execute(Ljava/lang/String;[Ljava/lang/String;)Z",
                                        "executeUpdate()I",
                                        "executeUpdate(Ljava/lang/String;)I",
                                        "executeUpdate(Ljava/lang/String;I)I",
                                        "executeUpdate(Ljava/lang/String;[I)I",
                                        "executeUpdate(Ljava/lang/String;[Ljava/lang/String;)I",
                                        "executeQuery()Ljava/sql/ResultSet;",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "executeBatch()[I",
                                        "executeBatchInternal()[J",
                                        "addBatch(Ljava/lang/String;)V"
                                }, new SqlStatementAdviceListener()))
                .onClass(new ClassMatcher("com/mysql/cj/jdbc/PreparedStatement")
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
                                        "executeBatchInternal()[J",
                                        "addBatch(Ljava/lang/String;)V"
                                }, new SqlStatementAdviceListener()))
                .onClass(new ClassMatcher("com.mysql.cj.jdbc.ClientPreparedStatement")
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
                                        "executeBatchInternal()[J",
                                        "addBatch(Ljava/lang/String;)V"
                                }, new SqlStatementAdviceListener()))
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
                params.put("server", "mysql");
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
                params.put("server", "mysql");
                params.put("type", "exception");
                params.put("exception", exception);
                params.put("jdbc_url", advice.getParameterArray()[0]);
                algorithmManager.doCheck(TYPE, context.get(), params);
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
            String query = Reflection.invokeStringMethod(self, "asSql", new Class[]{});
            if (query != null && !query.isEmpty()) {
                params.put("server", "mysql");
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
                if ("com.mysql.cj.jdbc.ClientPreparedStatement".equals(that.getClass().getName())) {
                    Object thisQuery = Reflection.getSuperField(that, "query");
                    query = (String) Reflection.invokeMethod(thisQuery, Class.forName("com.mysql.cj.PreparedQuery"), "getOriginalSql", new Class[0], new Object[0]);
                }
                /*else {
                    query = Reflection.invokeStringMethod(that, "originalSql", new Class[0]);
                }*/
            } catch (NullPointerException ignored) {
            }
            Throwable exception = advice.getThrowable();
            if (exception instanceof java.sql.SQLException) {
                params.put("server", "mysql");
                params.put("type", "exception");
                params.put("sql", query);
                params.put("exception", exception);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }
    }

    private class SqlConnectionAdviceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("server", "mysql");
            params.put("type", "connect");
            params.put("jdbc_url", advice.getParameterArray()[0]);
            algorithmManager.doCheck(TYPE, context.get(), params);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            if (SQLHook.disable) {
                return;
            }
            Map<String, Object> params = new HashMap<String, Object>();
            Throwable exception = advice.getThrowable();
            if (exception instanceof java.sql.SQLException) {
                params.put("server", "mysql");
                params.put("type", "exception");
                params.put("exception", exception);
                params.put("jdbc_url", advice.getParameterArray()[0]);
                algorithmManager.doCheck(TYPE, context.get(), params);
            }
        }
    }
}
