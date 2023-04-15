package com.jrasp.agent.module.sql.algorithm.impl;

import com.alibaba.druid.wall.WallUtils;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.LRUCache;
import com.jrasp.agent.api.util.ParamSupported;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * mysql注入检测算法
 */
public class MySqlAlgorithm implements Algorithm {
    /**
     * sql 最大长度
     */
    private Integer maxSqlLimitLength = 65535;

    /**
     * sql 最小长度
     */
    private Integer minSqlLimitLength = 16;

    /**
     * sql 注入检测的行为
     */
    private Integer mysqlBlockAction = 0;

    private final RaspLog logger;

    /**
     * sql白名单: 防止误报
     */
    private Set<String> mysqlWhiteSet = new HashSet<String>();

    private LRUCache<String, Object> SQL_LRU_CACHE = new LRUCache<String, Object>(1024);

    private RaspConfig raspConfig;

    public MySqlAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger) {
        this.logger = logger;
        this.minSqlLimitLength = ParamSupported.getParameter(configMaps, "mysql_min_limit_length", Integer.class, minSqlLimitLength);
        this.maxSqlLimitLength = ParamSupported.getParameter(configMaps, "mysql_max_limit_length", Integer.class, maxSqlLimitLength);
        this.mysqlBlockAction = ParamSupported.getParameter(configMaps, "mysql_block_action", Integer.class, mysqlBlockAction);
        this.mysqlWhiteSet = ParamSupported.getParameter(configMaps, "mysql_white_list", Set.class, mysqlWhiteSet);
        this.raspConfig = raspConfig;
    }

    public MySqlAlgorithm(RaspConfig raspConfig, RaspLog logger) {
        this.logger = logger;
        this.raspConfig = raspConfig;
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (mysqlBlockAction > -1) {
            if (parameters != null && parameters.length > 0) {
                String sql = (String) parameters[0];
                if (sql == null || "".equals(sql.trim())) {
                    return;
                }
                // 白名单放行
                if (mysqlWhiteSet.contains(sql)) {
                    return;
                }
                checkSql(sql, context);
            }
        }
    }

    @Override
    public String getType() {
        return "mysql";
    }

    @Override
    public String getDescribe() {
        return "no rule lexical analysis algorithm for mysql";
    }

    private void checkSql(String sql, Context context) throws Exception {
        if (sql != null && sql.length() >= minSqlLimitLength) {
            if (sql.length() > maxSqlLimitLength) {
                // 超过了最大长度，不检测
                // 不要截断sql，截断后的sql语句不完整，容易误报
                return;
            }
            // 先判断缓存
            boolean containsKey = SQL_LRU_CACHE.isContainsKey(sql);
            if (containsKey) {
                return;
            }
            boolean validateMySql = WallUtils.isValidateMySql(sql);
            if (validateMySql) {
                // 正常sql加入缓存，后面不再判断
                SQL_LRU_CACHE.put(sql, false);
                return;
            }
            // 判断阻断状态
            boolean enableBlock = mysqlBlockAction == 1;
            // 有行号栈，便于定位攻击链路
            AttackInfo attackInfo = new AttackInfo(context, sql, enableBlock, "sql inject", getDescribe(), "mysql", 95);
            // 记录日志日志
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("sql inject block by rasp."));
            }
        }
    }
}


