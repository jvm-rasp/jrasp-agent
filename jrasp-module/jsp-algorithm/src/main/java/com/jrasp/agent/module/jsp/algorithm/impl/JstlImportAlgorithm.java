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

import java.io.File;
import java.net.URI;
import java.util.Map;

public class JstlImportAlgorithm implements Algorithm {

    private final RaspLog logger;

    private RaspConfig raspConfig;

    private String metaInfo;

    private volatile Integer jstlImportAction = 0;

    public JstlImportAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public JstlImportAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.jstlImportAction = ParamSupported.getParameter(configMaps, "jstl_import_action", Integer.class, jstlImportAction);
    }

    @Override
    public String getType() {
        return "jstl-import";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        boolean enableBlock = jstlImportAction == 1;
        String url = (String) parameters[0];

        if (url != null && !url.startsWith("/") && url.contains("://")) {
            if (url.startsWith("file://")) {
                File realFile = new File(new URI(url));
                if (enableBlock && !realFile.exists()) {
                    return;
                }
                String realPath;
                try {
                    realPath = realFile.getCanonicalPath();
                } catch (Exception e) {
                    realPath = realFile.getAbsolutePath();
                }

                AttackInfo attackInfo = new AttackInfo(
                        context,
                        ClassPathUtil.getWebContext(),
                        metaInfo,
                        realPath,
                        enableBlock,
                        "JSTL标签引入",
                        getType(),
                        "detect jstl import, realPath: " + realPath,
                        100);
                logger.attack(attackInfo);
                if (enableBlock) {
                    ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("detect jstl tag import block by EpointRASP."));
                }
            }
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
        return "detect jstl import";
    }
}
