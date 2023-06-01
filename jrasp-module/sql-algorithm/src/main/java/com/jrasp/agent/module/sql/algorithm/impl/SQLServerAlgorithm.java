package com.jrasp.agent.module.sql.algorithm.impl;

import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.module.sql.hook.checker.util.SQLUtil;
import com.jrasp.agent.module.sql.hook.checker.util.TokenInfo;

import java.sql.SQLException;
import java.util.*;

public class SQLServerAlgorithm extends AbstractAlgorithm implements Algorithm {

    private static final List<String> defaultBlacklistFunction = new ArrayList<>(Arrays.asList("db_name", "col_name", "user_name", "host_name", "is_srvrolemember", "is_member", "opendatasource", "openrowset", "openquery", "has_dbaccess", "sp_executesql", "pg_sleep", "user", "system_user"));

    private static final List<String> defaultBlacklistSchema = new ArrayList<>(Arrays.asList("master", "sys", "sysobjects", "sysdatabases", "information_schema"));

    private static final List<String> defaultBlacklistTable = new ArrayList<>(Arrays.asList("sysaltfiles", "syslockinfo", "syscacheobjects", "syslogins", "syscharsets", "sysmessages", "sysconfigures", "sysoledbusers", "syscurconfigs", "sysperfinfo", "sysdatabases", "sysprocesses", "sysdevices", "sysremotelogins", "syslanguages", "sysservers", "syscolumns", "sysindexkeys", "syscomments", "sysmembers", "sysconstraints", "sysobjects", "sysdepends", "syspermissions", "sysfilegroups", "sysprotects", "sysfiles", "sysreferences", "sysforeignkeys", "systypes", "sysfulltextcatalogs", "sysusers", "sysindexes", "sysalerts", "sysjobsteps", "syscategories", "sysnotifications", "sysdownloadlist", "sysoperators", "sysjobhistory", "systargetservergroupmembers", "sysjobs", "systargetservergroups", "sysjobschedules", "systargetservers", "sysjobservers", "systaskids", "backupfile", "restorefile", "backupmediafamily", "restorefilegroup", "backupmediaset", "restorehistory", "backupset", "sysdatabases", "sysservers", "sysreplicationalerts", "msagent_parameters", "mspublisher_databases", "msagent_profiles", "msreplication_objects", "msarticles", "msreplication_subscriptions", "msdistpublishers", "msrepl_commands", "msdistributiondbs", "msrepl_errors", "msdistribution_agents", "msrepl_originators", "msdistribution_history", "msrepl_transactions", "msdistributor", "msrepl_version", "mslogreader_agents", "mssnapshot_agents", "mslogreader_history", "mssnapshot_history", "msmerge_agents", "mssubscriber_info", "msmerge_history", "mssubscriber_schedule", "msmerge_subscriptions", "mssubscriptions", "mspublication_access", "mssubscription_properties", "mspublications", "msmerge_contents", "sysmergearticles", "msmerge_delete_conflicts", "sysmergepublications", "msmerge_genhistory", "sysmergeschemachange", "msmerge_replinfo", "sysmergesubscriptions", "msmerge_tombstone", "sysmergesubsetfilters", "sysarticles", "syspublications", "sysarticleupdates", "syssubscriptions"));

    private static final List<String> defaultBlacklistVariant = new ArrayList<>(Arrays.asList("servername", "version", "user", "system_user"));

    public SQLServerAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.metaInfo = metaInfo;
        this.raspConfig = raspConfig;
        this.slowQueryCount = ParamSupported.getParameter(configMaps, "slow_query_count", Integer.class, slowQueryCount);
        this.slowQueryCheckAction = ParamSupported.getParameter(configMaps, "slow_query_check_action", Integer.class, slowQueryCheckAction);
        this.exceptionCheckAction = ParamSupported.getParameter(configMaps, "exception_check_action", Integer.class, exceptionCheckAction);
        this.regexCheckAction = ParamSupported.getParameter(configMaps, "regex_check_action", Integer.class, regexCheckAction);
        this.inputCheckAction = ParamSupported.getParameter(configMaps, "input_check_action", Integer.class, inputCheckAction);

        Set<String> sqlserverErrorCodeList = ParamSupported.getParameter(configMaps, "mssql_error_code_list", Set.class, new HashSet<>());
        List<String> tempList = new ArrayList<>(sqlserverErrorCodeList);
        errorCodes.put("mssql", new HashMap<>());
        errorCodes.get("mssql").put("error_code", tempList);

        this.blacklistFunction = ParamSupported.getParameter(configMaps, "mssql_blacklist_function", List.class, defaultBlacklistFunction);
        this.blacklistSchema = ParamSupported.getParameter(configMaps, "mssql_blacklist_schema", List.class, defaultBlacklistSchema);
        this.blacklistTable = ParamSupported.getParameter(configMaps, "mssql_blacklist_table", List.class, defaultBlacklistTable);
        this.blacklistVariant = ParamSupported.getParameter(configMaps, "mssql_blacklist_variant", List.class, defaultBlacklistVariant);
        blacklistStructure.put("or \\d+ = \\d+", "mysql");
    }

    public SQLServerAlgorithm(RaspConfig raspConfig, RaspLog logger) {
        this.logger = logger;
        this.raspConfig = raspConfig;
    }

    @Override
    public String getType() {
        return "sqlserver";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) parameters[0];

        // type = slowQuery exception injection
        String type = (String) params.getOrDefault("type", "");
        String sql = (String) params.getOrDefault("sql", "");
        String server = (String) params.getOrDefault("server", "");
        List<TokenInfo> tokens = SQLUtil.sqlTokenize(sql, type);
        String jdbcUrl = (String) params.getOrDefault("jdbc_url", "");
        Integer queryCount = (Integer) params.getOrDefault("query_count", -1);
        SQLException exception = (SQLException) params.getOrDefault("exception", null);

        switch (type) {
            case "slowQuery":
                slowQueryCheck(context, server, queryCount);
                break;
            case "exception":
                exceptionCheck(context, server, exception, sql);
                break;
            case "injection":
                if (userInputCheck(context, server, sql, tokens)) {
                    break;
                } else if (regexCheck(context, server, sql)) {
                    break;
                } else if (policyCheck(context, sql, server, tokens)) {
                    break;
                }
                break;
        }
    }

    @Override
    public String getDescribe() {
        return "no rule lexical analysis algorithm for sqlserver";
    }
}
