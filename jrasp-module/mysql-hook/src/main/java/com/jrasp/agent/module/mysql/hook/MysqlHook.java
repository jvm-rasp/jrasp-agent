package com.jrasp.agent.module.mysql.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * 兼容mysql8.0 mysql5.0
 */
@MetaInfServices(Module.class)
@Information(id = "mysql-hook", author = "jrasp")
public class MysqlHook implements Module, LoadCompleted {

    @RaspResource
    private RaspLog LOGGER;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String TYPE = "mysql";

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * sql 拼接 mysql8.x
                 * @see com.mysql.cj.jdbc.StatementImpl#executeInternal(String, boolean)
                 * @see com.mysql.cj.jdbc.StatementImpl#executeQuery(String)
                 * @see com.mysql.cj.jdbc.StatementImpl#executeUpdateInternal(String, boolean, boolean)
                 */
                .onClass(new ClassMatcher("com/mysql/cj/jdbc/StatementImpl")
                        .onMethod(
                                new String[]{
                                        "executeInternal(Ljava/lang/String;Z)Z",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "executeUpdateInternal(Ljava/lang/String;ZZ)J"
                                },
                                new SqlStatementAdviceListener()
                        )
                )
                /**
                 * sql 预编译语句 mysql8.x
                 * @see com.mysql.cj.jdbc.ClientPreparedStatement#execute()
                 * @see com.mysql.cj.jdbc.ClientPreparedStatement#executeQuery
                 * @see com.mysql.cj.jdbc.ClientPreparedStatement#executeUpdate()
                 * @see com.mysql.cj.jdbc.ClientPreparedStatement#executeBatchInternal()
                 * @see com.mysql.cj.jdbc.ClientPreparedStatement#executeBatch()
                 */
                .onClass(new ClassMatcher("com/mysql/cj/jdbc/ClientPreparedStatement")
                        .onMethod(new String[]{
                                "execute()Z",
                                "executeQuery()Ljava/sql/ResultSet;",
                                "executeUpdate()I",
                                "executeBatchInternal()[J",
                                "executeBatch()[I"
                        }, new Sql8PreparedStatementAdviceListener())
                )
                /**
                 * sql 拼接 mysql5.x
                 * @see com.mysql.jdbc.StatementImpl#executeInternal(String, boolean)
                 * @see com.mysql.jdbc.StatementImpl#executeQuery(String)
                 * @see com.mysql.jdbc.StatementImpl#executeUpdateInternal(String, boolean, boolean)
                 */
                .onClass(new ClassMatcher("com/mysql/jdbc/StatementImpl")
                        .onMethod(new String[]{
                                        "executeInternal(Ljava/lang/String;Z)Z",
                                        "executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;",
                                        "executeUpdateInternal(Ljava/lang/String;ZZ)J"},
                                new SqlStatementAdviceListener())
                )
                /**
                 * sql 预编译语句 mysql5.x
                 * @see com.mysql.jdbc.PreparedStatement#execute()
                 * @see com.mysql.jdbc.PreparedStatement#executeQuery
                 * @see com.mysql.jdbc.PreparedStatement#executeUpdate()
                 * @see com.mysql.jdbc.PreparedStatement#executeBatchInternal()
                 * @see com.mysql.jdbc.PreparedStatement#executeBatch()
                 */
                .onClass(new ClassMatcher("com/mysql/jdbc/PreparedStatement")
                        .onMethod(new String[]{
                                        "execute()Z",
                                        "executeQuery()Ljava/sql/ResultSet;",
                                        "executeUpdate()[J",
                                        "executeBatch()[I"
                                },
                                new Sql5StatementAdviceListener())
                )
                .build();
    }

    public class Sql8PreparedStatementAdviceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            com.mysql.cj.jdbc.ClientPreparedStatement preparedStatement = (com.mysql.cj.jdbc.ClientPreparedStatement) advice.getTarget();
            String sql = preparedStatement.getPreparedSql();
            algorithmManager.doCheck(TYPE, context.get(), sql);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            context.remove();
        }
    }

    public class SqlStatementAdviceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            String sql = (String) advice.getParameterArray()[0];
            algorithmManager.doCheck(TYPE, context.get(), sql);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            context.remove();
        }
    }

    public class Sql5StatementAdviceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            com.mysql.jdbc.PreparedStatement preparedStatement = (com.mysql.jdbc.PreparedStatement) advice.getTarget();
            String sql = preparedStatement.getPreparedSql();
            algorithmManager.doCheck(TYPE, context.get(), sql);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            context.remove();
        }
    }

}
