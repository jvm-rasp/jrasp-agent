package com.jrasp.agent.module.expression.algorithm.impl;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;

import java.util.Map;

/**
 * Ognl 检测算法
 *
 * @author jrasp
 */
public class OgnlAlgorithm implements Algorithm {

    /**
     * ognl表达式检测最小长度
     */
    private Integer ognlMinLength = 30;

    /**
     * ognl表达式限制最大长度
     */
    private Integer ognlMaxLimitLength = 400;

    /**
     * OGNL语句黑名单 检测算法的默认行为：记录
     */
    private Integer ognlBlackListAction = 0;

    /**
     * OGNL长度限制算法 检测算法的默认行为：记录
     */
    private Integer ognlMaxLimitLengthAction = 0;

    /**
     * OGNL语句黑名单
     */
    private String[] ognlBlackList = {
            "java.lang.Runtime",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.System",
            "java.lang.ProcessBuilder",
            "java.lang.Object",
            "java.lang.Shutdown",
            "ognl.OgnlContext",
            "ognl.TypeConverter",
            "ognl.MemberAccess",
            "_memberAccess",
            "ognl.ClassResolver",
            "java.io.File",
            "javax.script.ScriptEngineManager",
            "excludedClasses",
            "excludedPackageNamePatterns",
            "excludedPackageNames",
            "com.opensymphony.xwork2.ActionContext"
    };

    public static final String OGNL_BLOCK_MESSAGE = "";

    private final RaspLog logger;

    public OgnlAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public OgnlAlgorithm(RaspLog logger, Map<String, String> configMaps) {
        this.logger = logger;
        this.ognlMinLength = ParamSupported.getParameter(configMaps, "ognlMinLength", Integer.class, ognlMinLength);
        this.ognlMaxLimitLength = ParamSupported.getParameter(configMaps, "ognlMaxLimitLength", Integer.class, ognlMaxLimitLength);
        this.ognlBlackListAction = ParamSupported.getParameter(configMaps, "ognlBlackListAction", Integer.class, ognlBlackListAction);
        this.ognlMaxLimitLengthAction = ParamSupported.getParameter(configMaps, "ognlMaxLimitLengthAction", Integer.class, ognlMaxLimitLengthAction);
        this.ognlBlackList = ParamSupported.getParameter(configMaps, "ognlBlackList", String[].class, ognlBlackList);
    }

    @Override
    public String getType() {
        return "ognl";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (parameters[0] != null) {
            String expression = String.valueOf(parameters[0]);
            if (expression.length() >= ognlMinLength) {

                // 检测算法1: 黑名单
                if (this.ognlBlackListAction > -1) {
                    for (String s : ognlBlackList) {
                        if (expression.contains(s)) {
                            doAction(context, expression, ognlBlackListAction, "expression hit black list, black class: " + s, 90);
                            return;
                        }
                    }
                }

                // 检测算法2: 最大长度限制
                if (this.ognlMaxLimitLengthAction > -1) {
                    if (expression.length() >= ognlMaxLimitLength) {
                        doAction(context, expression, ognlBlackListAction, "the length of the expression exceeds the max length, length: " + expression.length(), 80);
                    }
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "ognl check algorithm";
    }

    private void doAction(Context context, String expression, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context, expression, enableBlock, getType(), getDescribe(), message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessControlException.throwThrowsImmediately(new RuntimeException("ognl expression block by rasp."));
        }
    }

}
