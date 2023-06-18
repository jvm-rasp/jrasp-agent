package com.jrasp.agent.module.jsp.algorithm.impl;

import com.epoint.core.utils.classpath.ClassPathUtil;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;

import java.util.Map;

public class JspCompileAlgorithm implements Algorithm {

    private final RaspLog logger;

    private RaspConfig raspConfig;

    private String metaInfo;

    private volatile Integer jspCompileAction = 0;

    public JspCompileAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public JspCompileAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.jspCompileAction = ParamSupported.getParameter(configMaps, "jsp_compile_action", Integer.class, jspCompileAction);
    }

    @Override
    public String getType() {
        return "jsp-compile";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        String requestURL = context.getRequestURL();
        if (requestURL.contains("check") || requestURL.contains("ewebeditor") || requestURL.contains("ueditor")
                || requestURL.contains("druid") || requestURL.contains("stimulsoftreport")) {
            return;
        }
        boolean enableBlock = jspCompileAction == 1;
        AttackInfo attackInfo = new AttackInfo(
                context,
                ClassPathUtil.getWebContext(),
                metaInfo,
                "",
                enableBlock,
                "JSP文件编译",
                getType(),
                getDescribe(),
                100);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("detect jsp file compile block by EpointRASP."));
        }
    }

    private boolean isWhiteList(Context context) {
        return context != null
                && StringUtils.isBlank(context.getMethod())
                && StringUtils.isBlank(context.getRequestURI())
                && StringUtils.isBlank(context.getRequestURL());
    }

    @Override
    public String getDescribe() {
        return "detect jsp file compile";
    }
}
