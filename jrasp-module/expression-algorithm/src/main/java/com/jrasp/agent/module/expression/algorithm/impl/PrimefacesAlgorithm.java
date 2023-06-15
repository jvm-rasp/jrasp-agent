package com.jrasp.agent.module.expression.algorithm.impl;

import com.epoint.core.utils.classpath.ClassPathUtil;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.Reflection;
import com.jrasp.agent.api.util.StringUtils;

import java.util.Map;

public class PrimefacesAlgorithm implements Algorithm {

    private final RaspLog logger;

    private RaspConfig raspConfig;

    private String metaInfo;

    private volatile Integer primefacesAction = 0;

    public PrimefacesAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public PrimefacesAlgorithm(RaspLog logger, RaspConfig raspConfig, Map<String, String> configMaps, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.primefacesAction = ParamSupported.getParameter(configMaps, "primefaces_action", Integer.class, primefacesAction);
    }

    @Override
    public String getType() {
        return "primefaces";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        Object facesContext = parameters[0];
        if (facesContext == null) {
            return;
        }
        boolean enableBlock = primefacesAction == 1;
        Object getExternalContext = Reflection.invokeMethod(facesContext, "getExternalContext", new Class[]{});
        Map<String, String> params = (Map<String, String>) Reflection.invokeMethod(getExternalContext, "getRequestParameterMap", new Class[]{});
        if (params.containsKey("primefacesDynamicContent")
                || params.containsKey("primefacesGraphicText")
                || params.containsKey("primefacesValidationCode")) {
            String message = "检测到 Primefaces EL 表达式注入漏洞利用";
            AttackInfo attackInfo = new AttackInfo(
                    context,
                    ClassPathUtil.getWebContext(),
                    metaInfo,
                    params.get("primefacesDynamicContent"),
                    enableBlock,
                    "EL表达式注入攻击",
                    "primefaces expression inject",
                    message,
                    100);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("primefaces expression block by EpointRASP."));
            }
        }
    }

    // 处理 Tomcat 启动时注入防护 Agent 产生的误报情况
    private boolean isWhiteList(Context context) {
        return context != null
                && StringUtils.isBlank(context.getMethod())
                && StringUtils.isBlank(context.getRequestURI())
                && StringUtils.isBlank(context.getRequestURL());
    }

    @Override
    public String getDescribe() {
        return "primefaces el check algorithm";
    }
}
