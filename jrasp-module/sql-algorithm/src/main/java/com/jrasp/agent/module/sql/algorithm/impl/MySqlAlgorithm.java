package com.jrasp.agent.module.sql.algorithm.impl;

import com.alibaba.druid.wall.WallUtils;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.LRUCache;
import com.jrasp.agent.api.util.ParamSupported;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * mysql注入检测算法
 */
public class MySqlAlgorithm implements Algorithm {


    private Boolean disable = false;

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
    private Integer action = 0;

    private final RaspLog logger;

    /**
     * sql白名单: 防止误报
     */
    private CopyOnWriteArraySet<String> sqlWhiteList = new CopyOnWriteArraySet<String>();

    private LRUCache<String, Object> SQL_LRU_CACHE = new LRUCache<String, Object>(1024);

    public MySqlAlgorithm(Map<String, String> configMaps, RaspLog logger) {
        this.logger = logger;
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        this.minSqlLimitLength = ParamSupported.getParameter(configMaps, "minSqlLimitLength", Integer.class, minSqlLimitLength);
        this.maxSqlLimitLength = ParamSupported.getParameter(configMaps, "maxSqlLimitLength", Integer.class, maxSqlLimitLength);
        this.action = ParamSupported.getParameter(configMaps, "action", Integer.class, action);
    }

    public MySqlAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (disable) {
            return;
        }
        if (parameters != null && parameters.length > 0) {
            String sql = (String) parameters[0];
            if (sql == null || "".equals(sql.trim())) {
                return;
            }
            // 白名单放行
            if (sqlWhiteList.contains(sql)) {
                return;
            }
            checkSql(sql, context);
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
            if (action > -1) {
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
                boolean enableBlock = action == 1;
                // 有行号栈，便于定位攻击链路
                AttackInfo attackInfo = new AttackInfo(context, sql, enableBlock, "sql inject", getDescribe(), "mysql", 95);
                // 记录日志日志
                logger.attack(attackInfo);
                if (enableBlock) {
                    ProcessController.throwsImmediately(new RuntimeException("sql inject block by rasp."));
                }
            }
        }
    }
}


