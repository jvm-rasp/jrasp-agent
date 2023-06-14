package com.jrasp.agent.module.sql.algorithm.impl;

import com.alibaba.druid.wall.WallUtils;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.LRUCache;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.module.sql.hook.checker.util.SQLUtil;
import com.jrasp.agent.module.sql.hook.checker.util.TokenInfo;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class MySqlAlgorithm extends AbstractAlgorithm implements Algorithm {

    private static final Set<String> defaultErrorCodeList = new HashSet<>(Arrays.asList("1060", "1062", "1064", "1105", "1367", "1037"));

    private static final List<String> defaultBlacklistFunction = new ArrayList<>(Arrays.asList("version", "load_file", "user", "system_user", "session_user", "benchmark", "current_user", "sleep", "xmltype", "receive_message", "pg_sleep", "is_srvrolemember", "updatexml", "extractvalue", "#hex", "#mid", "#ord", "#ascii", "#bin"));

    private static final List<String> defaultBlacklistKeyword = new ArrayList<>(Arrays.asList("#insert", "#select", "#delete", "#update", "#multi-insert", "#merge", "#call", "#truncate", "#crate", "#rename", "#alter", "#drop", "#set", "#replace", "#describe", "#show", "#commit", "#rollback", "#use", "#lock", "#unlock", "#begin transaction"));

    private static final List<String> defaultReadonlyTable = new ArrayList<>();

    private static final List<String> defaultBlacklistSchema = new ArrayList<>(Arrays.asList("mysql"));

    private static final List<String> defaultBlacklistVariant = new ArrayList<>(Arrays.asList("basedir", "version_compile_os", "version", "datadir"));

    private LRUCache<String, Object> SQL_LRU_CACHE = new LRUCache<String, Object>(1024);

    public MySqlAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.metaInfo = metaInfo;
        this.raspConfig = raspConfig;
        this.antiDetectPattern = Pattern.compile(ParamSupported.getParameter(configMaps, "anti_detect_regex", String.class, defaultDetectRegex));
        this.falsePositivePattern = Pattern.compile(ParamSupported.getParameter(configMaps, "positive_regex", String.class, defaultPositiveRegex));
        this.prefilterPattern = Pattern.compile(ParamSupported.getParameter(configMaps, "prefilter_regex", String.class, defaultPrefilterRegex));
        this.sqlRegexPattern = Pattern.compile(ParamSupported.getParameter(configMaps, "sql_regex", String.class, defaultSqlRegexRegex));
        this.exceptionCheckAction = ParamSupported.getParameter(configMaps, "exception_check_action", Integer.class, exceptionCheckAction);
        this.regexCheckAction = ParamSupported.getParameter(configMaps, "regex_check_action", Integer.class, regexCheckAction);
        this.inputCheckAction = ParamSupported.getParameter(configMaps, "input_check_action", Integer.class, inputCheckAction);

        Set<String> mysqlErrorCodeList = ParamSupported.getParameter(configMaps, "mysql_error_code_list", Set.class, defaultErrorCodeList);
        List<String> tempList = new ArrayList<>(mysqlErrorCodeList);
        errorCodes.put("mysql", new HashMap<>());
        errorCodes.get("mysql").put("error_code", tempList);

        this.blacklistFunction = ParamSupported.getParameter(configMaps, "mysql_blacklist_function", List.class, defaultBlacklistFunction);
        this.blackListKeyword = ParamSupported.getParameter(configMaps, "mysql_blacklist_keyword", List.class, defaultBlacklistKeyword);
        this.readonlyTable = ParamSupported.getParameter(configMaps, "mysql_readonly_table", List.class, defaultReadonlyTable);
        this.blacklistSchema = ParamSupported.getParameter(configMaps, "mysql_blacklist_schema", List.class, defaultBlacklistSchema);
        this.blacklistObject = ParamSupported.getParameter(configMaps, "mysql_blacklist_object", List.class, new ArrayList<>());
        this.blacklistTable = ParamSupported.getParameter(configMaps, "mysql_blacklist_table", List.class, new ArrayList<>());
        this.blacklistVariant = ParamSupported.getParameter(configMaps, "mysql_blacklist_table", List.class, defaultBlacklistVariant);
        blacklistStructure.put("or \\d+ = \\d+", "mysql");
    }

    public MySqlAlgorithm(RaspConfig raspConfig, RaspLog logger) {
        this.logger = logger;
        this.raspConfig = raspConfig;
    }

    @Override
    public String getType() {
        return "mysql";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) parameters[0];

        // type = exception injection
        String type = (String) params.getOrDefault("type", "");
        String sql = (String) params.getOrDefault("sql", "");

        boolean containsKey = SQL_LRU_CACHE.isContainsKey(sql);
        if (containsKey) {
            return;
        }

        String server = (String) params.getOrDefault("server", "");
        List<TokenInfo> tokens = SQLUtil.sqlTokenize(sql, type);
        String jdbcUrl = (String) params.getOrDefault("jdbc_url", "");
        SQLException exception = (SQLException) params.getOrDefault("exception", null);

        switch (type) {
            case "exception":
                if (isBehinderDatabaseBackdoor()) {
                    doActionCtl(1, context, String.format("server: %s, sql: %s, exception: %s", server, sql, exception.getMessage()), "detect behinder database backdoor", String.format("server: %s, sql: %s, exception: %s", server, sql, exception.getMessage()), 100);
                    return;
                }
                exceptionCheck(context, server, exception, sql);
                break;
            case "injection":
                if (isBehinderDatabaseBackdoor()) {
                    doActionCtl(1, context, String.format("server: %s, sql: %s", server, sql), "detect behinder database backdoor", String.format("server: %s, sql: %s", server, sql), 100);
                    return;
                }
                boolean isSqlInjection = userInputCheck(context, server, sql, tokens);
                if (!isSqlInjection) {
                    // 正常sql加入缓存，后面不再判断
                    SQL_LRU_CACHE.put(sql, false);
                    return;
                }
                break;
        }
    }

    @Override
    public String getDescribe() {
        return "no rule lexical analysis algorithm for mysql";
    }
}
