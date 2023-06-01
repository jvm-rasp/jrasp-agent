package com.jrasp.agent.module.sql.algorithm.impl;

import com.epoint.core.utils.classpath.ClassPathUtil;
import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.StackTrace;
import com.jrasp.agent.api.util.StringUtils;
import com.jrasp.agent.module.sql.algorithm.impl.checker.CheckerUtils;
import com.jrasp.agent.module.sql.algorithm.impl.checker.PolicyAction;
import com.jrasp.agent.module.sql.algorithm.impl.checker.ResultGroup;
import com.jrasp.agent.module.sql.algorithm.impl.checker.TokenCombine;
import com.jrasp.agent.module.sql.hook.checker.util.TokenInfo;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractAlgorithm {
    protected Pattern antiDetectPattern = Pattern.compile("add|all|alter|analyze|and|any|as|asc|avg|begin|between|by|case|create|count|delete|desc|do|dumpfile|else|elseif|end|exists|false|file|float|flush|follows|from|group|having|identified|if|in|insert|interval|into|join|last|like|limit|loop|not|null|on|or|order|procedure|regexp|return|rlike|select|then|true|union|update|values|xor");
    protected String defaultDetectRegex = "add|all|alter|analyze|and|any|as|asc|avg|begin|between|by|case|create|count|delete|desc|do|dumpfile|else|elseif|end|exists|false|file|float|flush|follows|from|group|having|identified|if|in|insert|interval|into|join|last|like|limit|loop|not|null|on|or|order|procedure|regexp|return|rlike|select|then|true|union|update|values|xor";
    protected Pattern falsePositivePattern = Pattern.compile("^(, *)?(([a-zA-Z_]\\w*|[0-9+\\-x\\.]+) *, *)+([a-zA-Z_]\\w*|[0-9+\\-x\\.]+)$");
    protected String defaultPositiveRegex = "^(, *)?(([a-zA-Z_]\\w*|[0-9+\\-x\\.]+) *, *)+([a-zA-Z_]\\w*|[0-9+\\-x\\.]+)$";
    protected Pattern prefilterPattern = Pattern.compile("select|file|from|;");
    protected String defaultPrefilterRegex = "select|file|from|;";
    protected Pattern sqlRegexPattern = Pattern.compile("");
    protected String defaultSqlRegexRegex = "";
    protected boolean userInputLcsSearchEnable = true;
    protected boolean userInputManagerAllow = true;

    protected boolean userInputPrefilterEnable = true;

    protected static final PolicyAction policyActionUnionMissingFrom = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionUnionMinusMissingFrom = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionUnionIntersectMissingFrom = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionUnionExpectMissingFrom = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionUnionNull = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionStackedQuery = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionNoHex = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionHint = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionIntoOutfile = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionInformationSchema = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionFunctionCount = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionBlacklistFunction = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionAt = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionSelectAll = PolicyAction.IGNORE;
    protected static final PolicyAction policyActionSelectInto = PolicyAction.LOG;
    protected static final PolicyAction policyActionUnionSelectHint = PolicyAction.LOG;
    protected static final PolicyAction policyActionBlacklistKeyword = PolicyAction.LOG;
    protected static final PolicyAction policyActionReadonlyTable = PolicyAction.LOG;
    protected static final PolicyAction policyActionBlacklistSchema = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionBlacklistObject = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionBlacklistTable = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionBlacklistVariant = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionBlacklistStructure = PolicyAction.BLOCK;
    protected static final PolicyAction policyActionDeleteNoneCondition = PolicyAction.LOG;
    protected static final PolicyAction policyActionUpdateNoneCondition = PolicyAction.LOG;
    protected static final PolicyAction mysqlBlacklistFunctionCount = PolicyAction.IGNORE;

    protected PolicyAction policyAction = PolicyAction.LOG;

    protected static final List<String> mysqlVariantWhiteList = new ArrayList<>(Arrays.asList(
            "auto_increment_increment", "auto_increment_offset", "autocommit", "automatic_sp_privileges",
            "character_set_client", "character_set_database", "character_set_connection", "character_set_results",
            "character-set-server", "collation_connection", "collation_database", "collation-server", "insert_id",
            "last_insert_id", "read_only", "sql_auto_is_null", "sql_big_selects", "sql_big_tables", "sql_mode",
            "tx_isolation", "warning_count", "identity"
    ));

    protected static final List<String> mysqlFunctionWhiteList = new ArrayList<>(Arrays.asList(
            "abs", "acos", "adddate", "addtime", "aes_decrypt", "aes_encrypt", "ascii", "asin", "atan", "atan2", "avg",
            "bin", "binary", "bit_and", "bit_count", "bit_length", "bit_or", "bit_xor", "cast", "ceil", "ceiling",
            "char_length", "char", "character_length", "charset", "coalesce", "coercibility", "collation", "compress",
            "concat_ws", "concat", "conv", "convert_tz", "convert", "cos", "cot", "count(distinct)", "count", "crc32",
            "curdate", "current_date", "current_time", "current_timestamp", "curtime", "date_add", "date_format",
            "date_sub", "date", "datediff", "day", "dayname", "dayofmonth", "dayofweek", "dayofyear", "decode",
            "default", "degrees", "des_decrypt", "des_encrypt", "div", "elt", "encode", "encrypt", "exp", "export_set",
            "extract", "field", "find_in_set", "floor", "format", "found_rows", "from_base64", "from_days",
            "from_unixtime", "get_format", "greatest", "group_concat", "gtid_subset", "gtid_subtract", "hex", "hour",
            "if", "ifnull", "inet_aton", "inet_ntoa", "inet6_aton", "inet6_ntoa", "insert", "instr", "interval",
            "is_free_lock", "is_ipv4_compat", "is_ipv4_mapped", "is_ipv4", "is_ipv6", "is_used_lock", "last_day",
            "last_insert_id", "lcase", "least", "left", "length", "ln", "localtime", "localtimestamp", "locate",
            "log10", "log2", "log", "lower", "lpad", "ltrim", "make_set", "makedate", "maketime", "master_pos_wait",
            "match", "max", "md5", "microsecond", "mid", "min", "minute", "mod", "month", "monthname", "name_const",
            "not", "now", "nullif", "oct", "octet_length", "old_password", "ord", "password", "period_add",
            "period_diff", "pi", "position", "pow", "power", "procedure", "quarter", "quote", "radians", "rand",
            "regexp", "release_lock", "repeat", "replace", "reverse", "right", "rlike", "round", "row_count", "rpad",
            "rtrim", "sec_to_time", "second", "sha1", "sha2", "sign", "sin", "soundex", "sounds", "space", "sqrt",
            "std", "stddev_pop", "stddev_samp", "stddev", "str_to_date", "strcmp", "subdate",
            "substr", "substring_index", "substring", "subtime", "sum", "sysdate", "tan", "time_format", "time_to_sec",
            "time", "timediff", "timestamp", "timestampadd", "timestampdiff", "to_base64", "to_days", "to_seconds",
            "trim", "truncate", "ucase", "uncompress", "uncompressed_length", "unhex", "unix_timestamp", "upper",
            "utc_date", "utc_time", "utc_timestamp", "uuid_short", "uuid", "validate_password_strength", "values",
            "var_pop", "var_samp", "variance", "wait_until_sql_thread_after_gtids", "week", "weekday",
            "weekofyear", "weight_string", "year", "yearweek"
    ));

    protected List<String> blacklistFunction = new ArrayList<>();

    protected List<String> blackListKeyword = new ArrayList<>();

    protected List<String> readonlyTable = new ArrayList<>();

    protected List<String> blacklistSchema = new ArrayList<>();

    protected List<String> blacklistObject = new ArrayList<>();

    protected List<String> blacklistTable = new ArrayList<>();

    protected List<String> blacklistVariant = new ArrayList<>();

    protected Map<String, Object> blacklistStructure = new HashMap<>();
    protected Map<String, Integer> blackListFunctionCount = new HashMap<>();

    protected static final Pattern MYSQL_1064_PATTERN_IN = Pattern.compile("in\\s*(\\(\\s*\\)|[^(\\w])", Pattern.CASE_INSENSITIVE);

    protected static Map<String, Map<String, List<String>>> errorCodes = new HashMap<>();

    protected static final String SqlUnionMissingFromRule = "2500161001";

    protected static final String SqlMinusMissingFromRule = "2500161002";

    protected static final String SqlIntersectMissingFromRule = "2500161003";

    protected static final String SqlExceptMissingFromRule = "2500161004";

    protected static final String SqlUnionNullRule = "2500161005";

    protected static final String SqlStackRule = "2500161006";

    protected static final String SqlHexadecimalRule = "2500161007";

    protected static final String SqlMySqlVersionCommentRule = "2500161008";

    protected static final String SqlFunctionBlacklistRule = "2500161009";

    protected static final String SqlFunctionBlacklistCountRule = "2500161010";

    protected static final String SqlIntoOutfileRule = "2500161011";

    protected static final String SqlInformationSchemaRule = "2500161012";

    protected static final String SqlAtRule = "2500161013";

    protected static final String SqlSelectStarRule = "2500161014";

    protected static final String SqlSelectIntoRule = "2500161015";

    protected static final String SqlSelectHintRule = "2500161016";

    protected static final String SqlKeywordBlacklistRule = "2500161017";

    protected static final String SqlKeywordCombineBlacklistRule = "2500161018";

    protected static final String SqlTableReadOnlyRule = "2500161019";

    protected static final String SqlSchemaBlacklistRule = "2500161020";

    protected static final String SqlObjectBlacklistRule = "2500161021";

    protected static final String SqlTableBlacklistRule = "2500161022";

    protected static final String SqlVariableBlacklistRule = "2500161023";

    protected static final String SqlStructureBlacklistRule = "2500161024";

    protected static final String SqlDeleteNoConditionRule = "2500161025";

    protected static final String SqlUpdateNoConditionRule = "2500161026";

    protected RaspLog logger;

    protected RaspConfig raspConfig;

    protected String metaInfo;
    protected volatile Integer slowQueryCheckAction = 0;
    protected volatile Integer slowQueryCount = 3000;
    protected volatile Integer exceptionCheckAction = 0;
    protected volatile Integer regexCheckAction = 0;
    protected volatile Integer inputCheckAction = 0;

    protected void doActionCtl(int action, Context context, String payload, String algorithm, String message, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, ClassPathUtil.getWebContext(), metaInfo, payload, enableBlock, "冰蝎数据库管理后门", algorithm, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("behinder backdoor block by EpointRASP."));
            }
        }
    }

    protected void slowQueryCheck(Context context, String server, Integer queryCount) throws ProcessControlException {
        boolean enableBlock = slowQueryCheckAction == 1;
        if (queryCount >= slowQueryCount) {
            AttackInfo attackInfo = new AttackInfo(
                    context,
                    ClassPathUtil.getWebContext(),
                    metaInfo,
                    String.valueOf(queryCount),
                    enableBlock,
                    "SQL查询结果集过大",
                    "slowQuery",
                    "sql result set greater than " + slowQueryCount,
                    50);
            logger.attack(attackInfo);
        }
    }

    protected boolean regexCheck(Context context, String server, String sql) throws ProcessControlException {
        boolean enableBlock = regexCheckAction == 1;
        if (StringUtils.isBlank(sqlRegexPattern.pattern())) {
            return false;
        }
        if (sqlRegexPattern.matcher(sql).find()) {
            AttackInfo attackInfo = new AttackInfo(
                    context,
                    ClassPathUtil.getWebContext(),
                    metaInfo,
                    sql,
                    false,
                    "SQL注入",
                    "sqlInjection(black regex)",
                    "sql injection attack(black regex), sql: " + sql,
                    100);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql injection(black regex) block by EpointRASP."));
            } else {
                return true;
            }
        }
        return false;
    }

    protected boolean userInputCheck(Context context, String server, String sql, List<TokenInfo> tokens) throws ProcessControlException {
        boolean enableBlock = inputCheckAction == 1;
        Map<String, String[]> params = context.getDecryptParameters();
        if (params == null) {
            return false;
        }
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String value = lexicalAnalyzer(entry.getValue(), entry.getKey(), sql, tokens);
            if (value != null) {
                if (value.startsWith("{\"pageUrl\":\"")) {
                    continue;
                }
                AttackInfo attackInfo = new AttackInfo(
                        context,
                        ClassPathUtil.getWebContext(),
                        metaInfo,
                        value,
                        false,
                        "SQL注入",
                        "sqlInjection(input param check)",
                        "sql injection attack, sql: " + sql,
                        100);
                logger.attack(attackInfo);
                if (enableBlock) {
                    ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql injection(input param check) block by EpointRASP."));
                } else {
                    return true;
                }
            }
        }
        Map<String, String> headers = context.getHeader();
        if (headers == null) {
            return false;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            // 取消header头检测
            /*if (entry.getKey().equalsIgnoreCase("referer")
                    || entry.getKey().equalsIgnoreCase("user-agent")
                    || entry.getKey().equalsIgnoreCase("x-forwarded-for")) {
                String ret = lexicalAnalyzer(new String[]{entry.getValue()}, "header: " + entry.getKey(), sql, tokens);
                if (ret != null) {
                    AttackInfo attackInfo = new AttackInfo(
                            context,
                            ret,
                            false,
                            "sql injection(input header check)",
                            "sqlInjection",
                            "sql injection attack, sql: " + sql,
                            100);
                    logger.attack(attackInfo);
                    if (enableBlock) {
                        ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql injection(input header check) block by EpointRASP."));
                    }
                }
                continue;
            }*/
            if (entry.getKey().equalsIgnoreCase("cookie")) {
                String[] cookies = entry.getValue().split(";");
                for (String cookie : cookies) {
                    String[] keyValue = cookie.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        String ret = lexicalAnalyzer(new String[]{value}, "cookie:" + key, sql, tokens);
                        if (ret != null) {
                            AttackInfo attackInfo = new AttackInfo(
                                    context,
                                    ClassPathUtil.getWebContext(),
                                    metaInfo,
                                    ret,
                                    false,
                                    "SQL注入",
                                    "sqlInjection(input cookie check)",
                                    "sql injection attack, sql: " + sql,
                                    100);
                            logger.attack(attackInfo);
                            if (enableBlock) {
                                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql injection(input cookie check) block by EpointRASP."));
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static final List<TokenCombine> CHECKING_KEYWORDS_COMBINE_LIST = new LinkedList<>();
    private static final List<TokenCombine> CHECKING_STRUCTURE_COMBINE_LIST = new LinkedList<>();

    protected boolean policyCheck(Context context, String sql, String server, List<TokenInfo> tokens) throws ProcessControlException {
        List<ResultGroup> resultGroups = checkPolicy(server, tokens);
        if (resultGroups.isEmpty()) {
            return false;
        }
        ResultGroup resultGroup = resultGroups.get(0);
        if (resultGroup.messageId == null) {
            return false;
        }
        for (ResultGroup group : resultGroups) {
            boolean enableBlock = group.action.isBlock();
            AttackInfo attackInfo = new AttackInfo(
                    context,
                    ClassPathUtil.getWebContext(),
                    metaInfo,
                    sql,
                    enableBlock,
                    "SQL注入",
                    "sqlInjectionPolicyCheck",
                    String.format("sql injection attack: %s %s , sql: %s", resultGroup.actionDataKey, resultGroup.actionDataValue, sql),
                    100);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql injection(policy) block by EpointRASP."));
            } else {
                return true;
            }
        }
        return false;
    }

    protected List<ResultGroup> checkPolicy(String server, List<TokenInfo> tokens) {
        List<String> tokensLowerCase = new ArrayList<>(tokens.size());
        for (TokenInfo token : tokens) {
            tokensLowerCase.add(token.text.substring(0, Math.min(50, token.text.length())).toLowerCase());
        }
        boolean unionState = false;
        Map<String, Integer> funcCount = new HashMap<>();
        boolean select = false;
        boolean from = false;
        boolean where = false;
        boolean union = false;
        boolean delete = false;
        boolean update = false;
        boolean insert = false;
        boolean into = false;
        boolean minus = false;
        boolean except = false;
        boolean intersect = false;
        List<TokenCombine> tokenCombinesRemove = new LinkedList<>();
        List<ResultGroup> result = new LinkedList<>();
        for (int i = 0; i < tokensLowerCase.size(); i++) {
            String token = tokensLowerCase.get(i);
            if ("select".equals(token)) {
                select = true;
            } else if ("from".equals(token)) {
                from = true;
            } else if ("where".equals(token)) {
                where = true;
            } else if ("union".equals(token)) {
                if (!policyActionUnionMissingFrom.isIgnore() && union && !from
                        && appendResultGroup(policyActionUnionMissingFrom, "ristStructure", "union (missing from)", SqlUnionMissingFromRule, result)) {
                    return result;
                }
                union = true;
                select = false;
                where = false;
                from = false;
                into = false;
            } else if ("minus".equals(token)) {
                if (!policyActionUnionMinusMissingFrom.isIgnore() && minus && !from
                        && appendResultGroup(policyActionUnionMinusMissingFrom, "riskStructure", "minus (missing from)", SqlMinusMissingFromRule, result)) {
                    return result;
                }
                minus = true;
                select = false;
                where = false;
                from = false;
                into = false;
            } else if ("intersect".equals(token)) {
                if (!policyActionUnionIntersectMissingFrom.isIgnore() && intersect && !from &&
                        appendResultGroup(policyActionUnionIntersectMissingFrom, "riskStructure", "intersect (missing from)", SqlIntersectMissingFromRule, result)) {
                    return result;
                }
                intersect = true;
                select = false;
                where = false;
                from = false;
                into = false;
            } else if ("except".equals(token)) {
                if (!policyActionUnionExpectMissingFrom.isIgnore() && except && !from &&
                        appendResultGroup(policyActionUnionExpectMissingFrom, "riskStructure", "except (missing from)", SqlExceptMissingFromRule, result)) {
                    return result;
                }
                except = true;
                select = false;
                where = false;
                from = false;
                into = false;
            } else if ("delete".equals(token)) {
                delete = true;
            } else if ("update".equals(token)) {
                update = true;
            } else if ("insert".equals(token)) {
                insert = true;
            } else if ("into".equals(token)) {
                into = true;
            }
            if (!policyActionUnionNull.isIgnore()) {
                if (token.equals("union")) {
                    unionState = true;
                } else if (token.equals("from")) {
                    unionState = false;
                } else if (token.equals("select") && unionState) {
                    int nullCount = 0;
                    int numCount = 0;
                    int j;
                    int tmp = 0;
                    for (j = i + 1; j < tokensLowerCase.size() && j < i + 6; tmp++) {
                        if (tmp > 10) {
                            break;
                        }
                        String tokenJ = tokensLowerCase.get(j);
                        String tokenJ1 = (j < tokensLowerCase.size() - 1) ? tokensLowerCase.get(j + 1) : null;
                        if ((tokenJ.equals(",") || tokenJ.equals("null")) && !tokenJ.equals(tokenJ1)) {
                            nullCount++;
                            j++;
                        }
                    }
                    tmp = 0;
                    for (j = i + 1; j < tokensLowerCase.size() && j < i + 6; tmp++) {
                        if (tmp > 10) {
                            break;
                        }
                        String tokenJ = tokensLowerCase.get(j);
                        String tokenJ1 = (j < tokensLowerCase.size() - 1) ? tokensLowerCase.get(j + 1) : null;
                        boolean isNum = false;
                        try {
                            Integer.parseInt(tokenJ);
                            isNum = true;
                        } catch (NumberFormatException ignored) {
                        }
                        if ((tokenJ.equals(",") || isNum) && !tokenJ.equals(tokenJ1)) {
                            numCount++;
                            j++;
                        }
                    }
                    if (nullCount >= 5 || numCount >= 5) {
                        String actionDataValue;
                        if (nullCount >= 5) {
                            actionDataValue = "union NULL, NULL, NULL";
                        } else {
                            actionDataValue = "union 1, 2, 3";
                        }
                        if (appendResultGroup(policyActionUnionNull, "blackStructure", actionDataValue, SqlUnionNullRule, result)) {
                            return result;
                        }
                    }
                }
                if (!policyActionStackedQuery.isIgnore() && token.equals(";") && i != tokensLowerCase.size() - 1 &&
                        appendResultGroup(policyActionStackedQuery, "riskStructure", ";", SqlStackRule, result)) {
                    {
                        return result;
                    }
                }
                if (!policyActionNoHex.isIgnore() && token.startsWith("0x") &&
                        appendResultGroup(policyActionNoHex, "riskStructure", "0x", SqlHexadecimalRule, result)) {
                    {
                        return result;
                    }
                }
                if (!policyActionHint.isIgnore() && token.startsWith("/*!") &&
                        appendResultGroup(policyActionHint, "riskStructure", "/*!", SqlMySqlVersionCommentRule, result)) {
                    {
                        return result;
                    }
                }
                if (i > 0 && token.startsWith("(")) {
                    String funcName = tokensLowerCase.get(i - 1);
                    if (!policyActionBlacklistFunction.isIgnore() && blacklistFunction.contains(funcName) &&
                            appendResultGroup(policyActionBlacklistFunction, "blackFunction", funcName + "()", SqlFunctionBlacklistRule, result)) {
                        return result;
                    }
                    // 应该指的是对应函数出现的次数
                    /*if (!policyActionFunctionCount.isIgnore() && (Integer) ConfigUtils.getOrDefault(mysqlBlacklistFunctionCount, funcName, 0) > 0) {
                        Integer count = funcCount.get(funcName);
                        if (count == null){
                            count = 0;
                        }
                        funcCount.put(funcName, count + 1);
                        if (funcCount.get(funcName) >= Config.sqlServerBlacklistFunctionCount(server).get(funcName) &&
                                appendResultGroup(policyActionFunctionCount, "riskFunction", funcName + "()", SqlFunctionBlacklistCountRule, result))
                            return result;
                    }*/
                }
                if (!policyActionIntoOutfile.isIgnore() && i < tokensLowerCase.size() - 2 && token
                        .equals("into") && (tokensLowerCase
                        .get(i + 1).equals("outfile") || tokensLowerCase
                        .get(i + 1).equals("dumpfile"))) {
                    if (appendResultGroup(policyActionIntoOutfile, "riskStructure", "into " + tokensLowerCase
                            .get(i + 1), SqlIntoOutfileRule, result)) {
                        {
                            return result;
                        }
                    }
                }
                if (!policyActionInformationSchema.isIgnore() && i < tokensLowerCase.size() - 1 && token.equals("from")) {
                    String part = tokensLowerCase.get(i + 1).replace("`", "");
                    if (part.equals("information_schema.tables")) {
                        if (appendResultGroup(policyActionInformationSchema, "riskTable", "information_schema.tables", SqlInformationSchemaRule, result)) {
                            return result;
                        }
                    } else if (part.equals("information_schema") && i < tokensLowerCase.size() - 3) {
                        String part2 = tokensLowerCase.get(i + 3).replace("`", "");
                        if (part2.equals("tables") &&
                                appendResultGroup(policyActionInformationSchema, "riskTable", "information_schema.tables", SqlInformationSchemaRule, result)) {
                            return result;
                        }
                    }
                }
                if (!policyActionAt.isIgnore() && token.equals("@") && select && from &&
                        appendResultGroup(policyActionAt, "riskStructure", "@", SqlAtRule, result)) {
                    {
                        return result;
                    }
                }
                if (!policyActionSelectAll.isIgnore() && token.equals("*") && select && !from &&
                        appendResultGroup(policyActionSelectAll, "riskStructure", "select *", SqlSelectStarRule, result)) {
                    {
                        return result;
                    }
                }
                if (!policyActionSelectInto.isIgnore() && token.equals("into") && select && !from &&
                        appendResultGroup(policyActionSelectInto, "riskStructure", "select into", SqlSelectIntoRule, result)) {
                    {
                        return result;
                    }
                }
                if (!policyActionUnionSelectHint.isIgnore() && token.startsWith("/*!") && union && select &&
                        appendResultGroup(policyActionUnionSelectHint, "riskStructure", "union select /*!", SqlSelectHintRule, result)) {
                    {
                        return result;
                    }
                }
                if (!policyActionBlacklistKeyword.isIgnore()) {
                    if (blackListKeyword.contains(token) && tokensLowerCase.size() > i + 1
                            && tokensLowerCase.get(i + 1).equals("(")) {
                        if (appendResultGroup(policyActionBlacklistKeyword, "blackKeyword", token, SqlKeywordBlacklistRule, result)) {
                            return result;
                        }
                    } else {
                        Map<String, Object> map = new HashMap<>();
                        blackListKeyword.forEach(it -> map.put(it, "mysql"));
                        if (map.get(token) instanceof TokenCombine) {
                            TokenCombine tokenCombine = (TokenCombine) map.get(token);
                            if (tokenCombine != null) {
                                CHECKING_KEYWORDS_COMBINE_LIST.add(tokenCombine.copy());
                            }
                        }
                    }
                    for (TokenCombine tokenCombine : CHECKING_KEYWORDS_COMBINE_LIST) {
                        int ret = tokenCombine.feed(token);
                        if (ret == 0) {
                            tokenCombinesRemove.add(tokenCombine);
                        }
                        if (ret == 2 &&
                                appendResultGroup(policyActionBlacklistKeyword, "blackKeyword", tokenCombine
                                        .getMatches(), SqlKeywordCombineBlacklistRule, result)) {
                            {
                                return result;
                            }
                        }
                    }
                    for (TokenCombine tokenCombine : tokenCombinesRemove) {
                        CHECKING_KEYWORDS_COMBINE_LIST.remove(tokenCombine);
                    }
                    tokenCombinesRemove.clear();
                }
                if (!policyActionReadonlyTable.isIgnore() && readonlyTable.contains(token)
                        && !where && (update || delete || insert || (select && into))) {
                    if (appendResultGroup(policyActionReadonlyTable, "riskTable", token, SqlTableReadOnlyRule, result)) {
                        return result;
                    }
                }
                if (!policyActionBlacklistSchema.isIgnore() && blacklistSchema.contains(token)) {
                    if (appendResultGroup(policyActionBlacklistSchema, "blackSchema", token, SqlSchemaBlacklistRule, result)) {
                        return result;
                    }
                }
                if (!policyActionBlacklistObject.isIgnore() && blacklistObject.contains(token)) {
                    if (appendResultGroup(policyActionBlacklistObject, "blackObject", token, SqlObjectBlacklistRule, result)) {
                        return result;
                    }
                }
                if (!policyActionBlacklistTable.isIgnore() && blacklistTable.contains(token)) {
                    if (appendResultGroup(policyActionBlacklistVariant, "blackTable", token, SqlTableBlacklistRule, result)) {
                        return result;
                    }
                }
                if (!policyActionBlacklistVariant.isIgnore() && i >= 2 && tokensLowerCase
                        .get(i - 2).equals("@") && tokensLowerCase
                        .get(i - 1).equals("@") &&
                        blacklistVariant.contains(token)) {
                    if (appendResultGroup(policyActionBlacklistVariant, "blackVariable", "@@" + token, SqlVariableBlacklistRule, result)) {
                        return result;
                    }
                }
                if (!policyActionBlacklistStructure.isIgnore()) {
                    if (blacklistStructure.get(token) != null) {
                        CHECKING_STRUCTURE_COMBINE_LIST.add(((TokenCombine) blacklistStructure.get(token)).copy());
                    }
                    for (TokenCombine tokenCombine : CHECKING_STRUCTURE_COMBINE_LIST) {
                        int ret = tokenCombine.feed(token);
                        if (ret == 0) {
                            tokenCombinesRemove.add(tokenCombine);
                        }
                        if (ret == 2 &&
                                appendResultGroup(policyActionBlacklistStructure, "blackStructure", tokenCombine
                                        .getMatches(), SqlStructureBlacklistRule, result)) {
                            return result;
                        }
                    }
                    for (TokenCombine tokenCombine : tokenCombinesRemove) {
                        CHECKING_STRUCTURE_COMBINE_LIST.remove(tokenCombine);
                    }
                    tokenCombinesRemove.clear();
                }
            }
        }
        if (!policyActionDeleteNoneCondition.isIgnore() && delete && !where &&
                appendResultGroup(policyActionDeleteNoneCondition, "riskStructure", "delete (missing where)", SqlDeleteNoConditionRule, result)) {
            return result;
        }
        if (!policyActionUpdateNoneCondition.isIgnore() && update && !where &&
                appendResultGroup(policyActionUpdateNoneCondition, "riskStructure", "update (missing where)", SqlUpdateNoConditionRule, result)) {
            return result;
        }
        if (!policyActionUnionMissingFrom.isIgnore() && union && !from &&
                appendResultGroup(policyActionUnionMissingFrom, "riskStructure", "union (missing from)", SqlUnionMissingFromRule, result)) {
            return result;
        }
        if (!policyActionUnionMinusMissingFrom.isIgnore() && minus && !from &&
                appendResultGroup(policyActionUnionMinusMissingFrom, "riskStructure", "minus (missing from)", SqlMinusMissingFromRule, result)) {
            return result;
        }
        if (!policyActionUnionIntersectMissingFrom.isIgnore() && intersect && !from &&
                appendResultGroup(policyActionUnionIntersectMissingFrom, "riskStructure", "intersect (missing from)", SqlIntersectMissingFromRule, result)) {
            return result;
        }
        if (!policyActionUnionExpectMissingFrom.isIgnore() && except && !from &&
                appendResultGroup(policyActionUnionExpectMissingFrom, "riskStructure", "except (missing from)", SqlExceptMissingFromRule, result)) {
            return result;
        }
        return result;
    }

    protected boolean appendResultGroup(PolicyAction action, String actionDataKey, String actionDataValue, String messageId, List<ResultGroup> resultGroupList) {
        ResultGroup resultGroup = new ResultGroup();
        resultGroup.action = action;
        resultGroup.actionDataKey = actionDataKey;
        resultGroup.actionDataValue = actionDataValue;
        resultGroup.messageId = messageId;
        if (policyAction.compareTo(action) <= 0) {
            resultGroup.action = policyAction;
            resultGroupList.clear();
            resultGroupList.add(resultGroup);
            return true;
        }
        resultGroupList.add(resultGroup);
        return false;
    }

    protected boolean isBehinderDatabaseBackdoor() {
        String[] stacks = StackTrace.getStackTraceString();
        boolean flag1, flag2 = false;
        for (int i = 0; i < stacks.length; i++) {
            flag1 = stacks[i].contains("executeSQL") && stacks[i].contains("Database");
            if (stacks.length > i + 1) {
                flag2 = stacks[i + 1].contains("equals") && stacks[i + 1].contains("Database");
            }
            if (flag1 && flag2) {
                return true;
            }
        }
        return false;
    }

    protected void exceptionCheck(Context context, String server, SQLException exception, String sql) throws ProcessControlException {
        boolean enableBlock = exceptionCheckAction == 1;
        if (server == null || exception == null || sql == null || server.isEmpty()) {
            return;
        }
        String errorCode = getError(server, exception);
        if (errorCode == null) {
            return;
        }
        String message = exception.getMessage();
        if (server.equals("mysql")) {
            if (errorCode.equals("1062")) {
                if (!sql.toLowerCase().contains("rand")) {
                    return;
                }
            } else if (errorCode.equals("1064")) {
                if (MYSQL_1064_PATTERN_IN.matcher(sql).find()) {
                    return;
                }
                if (!message.toLowerCase().contains("syntax")) {
                    return;
                }
            }
        }

        AttackInfo attackInfo = new AttackInfo(
                context,
                ClassPathUtil.getWebContext(),
                metaInfo,
                errorCode,
                enableBlock,
                "SQL语句异常",
                "sqlException",
                "sql exception, errorCode: " + errorCode + ", errorMessage: " + message,
                50);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql injection(exception) block by EpointRASP."));
        }
    }

    // 处理 Tomcat 启动时注入防护 Agent 产生的误报情况
    protected boolean isWhiteList(Context context) throws ProcessControlException {
        return context != null
                && StringUtils.isBlank(context.getMethod())
                && StringUtils.isBlank(context.getRequestURI())
                && StringUtils.isBlank(context.getRequestURL());
    }

    protected String lexicalAnalyzer(String[] values, String name, String query, List<TokenInfo> tokens) {
        Queue<String> queue = new LinkedList<>(Arrays.asList(values));
        while (!queue.isEmpty()) {
            String checkValue[], value = queue.remove();
            if (value.length() < 3) {
                continue;
            }
            String decoded = CheckerUtils.tryDecodeString(value);
            if (decoded != null) {
                queue.add(decoded);
            }
            if (!userInputLcsSearchEnable) {
                checkValue = new String[]{value};
            } else {
                checkValue = lcsSearch(query, value);
            }
            for (String cValue : checkValue) {
                if (cValue.length() >= 3) {
                    int userInputIndex = query.indexOf(cValue);
                    if (userInputIndex != -1)
                        if (!userInputManagerAllow || query.length() != cValue.length()) {
                            if (!falsePositivePattern.matcher(cValue).find()) {
                                if (!userInputPrefilterEnable || !prefilterPattern.matcher(cValue).find()) {
                                    int distance = 2;
                                    if (cValue.length() > 20) {
                                        distance = 3;
                                    }
                                    if (isTokenChanged(userInputIndex, cValue.length(), distance, tokens)) {
                                        return value;
                                    }
                                }
                            }
                        }
                }
            }
        }
        return null;
    }

    protected static String[] lcsSearch(String str1, String str2) {
        int len1 = str1.length(), len2 = str2.length();
        int[][] dp = {new int[len2 + 2], new int[len2 + 2]};
        int pre = 1, now = 0, matchedLength = 0;
        Set<Integer> matchedStartPos = new HashSet<>();
        int i;
        for (i = 0; i <= len2 + 1; i++) {
            dp[0][i] = 0;
            dp[1][i] = 0;
        }
        for (i = 0; i <= len1; i++) {
            for (int k = 0; k <= len2; k++) {
                if (i == 0 || k == 0) {
                    dp[now][k] = 0;
                } else if (str1.charAt(i - 1) == str2.charAt(k - 1)) {
                    dp[now][k] = dp[pre][k - 1] + 1;
                    if (dp[now][k] > matchedLength) {
                        matchedLength = dp[now][k];
                        matchedStartPos.clear();
                        matchedStartPos.add(i - matchedLength);
                    } else if (dp[now][k] == matchedLength) {
                        matchedStartPos.add(i - matchedLength);
                    }
                } else {
                    dp[now][k] = 0;
                }
            }
            now ^= 0x1;
            pre ^= 0x1;
        }
        String[] result = new String[matchedStartPos.size()];
        int j = 0;
        for (Integer pos : matchedStartPos) {
            result[j++] = str1.substring(pos, pos + matchedLength);
        }
        return result;
    }

    protected boolean isTokenChanged(int userInputIndex, int userInputLength, int distance, List<TokenInfo> tokens) {
        int start = -1, end = tokens.size() - 1, userInputEndIndex = userInputIndex + userInputLength;
        int i;
        for (i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).stop > userInputIndex) {
                start = i;
                break;
            }
        }
        if (start == -1) {
            return false;
        }
        if (tokens.get(start).stop >= userInputEndIndex) {
            end = start;
        } else {
            for (i = start + 1; i < tokens.size(); i++) {
                if (tokens.get(i).stop >= userInputEndIndex) {
                    if (tokens.get(i).start >= userInputEndIndex) {
                        end = i - 1;
                        break;
                    }
                    end = i;
                    break;
                }
            }
        }
        int diff = end - start + 1;
        if (diff >= distance) {
            if (diff < 10) {
                int nonKeyword = 0;
                for (int j = start; j <= end; j++) {
                    if (!antiDetectPattern.matcher(tokens.get(j).text).find()) {
                        nonKeyword++;
                    }
                    if (nonKeyword >= 2) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
        return false;
    }

    protected String getError(String server, SQLException exception) {
        int code = Math.abs(exception.getErrorCode());
        String errorCode = String.valueOf(code);
        if (errorCodes.containsKey(server) && errorCodes.get(server).containsKey("error_code") && ((List<?>) ((Map<?, ?>) errorCodes
                .get(server)).get("error_code")).contains(errorCode)) {
            return errorCode;
        }
        String errorState = exception.getSQLState();
        if (errorCodes.containsKey(server) && errorCodes.get(server).containsKey("error_state") && ((List<?>) ((Map<?, ?>) errorCodes
                .get(server)).get("error_state")).contains(errorState)) {
            return errorState;
        }
        return null;
    }
}
