package com.jrasp.agent.module.file.algorithm.impl;

import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;

import java.util.Map;

/**
 * @author jrasp
 * 路径穿越文件删除
 */
public class FileInitAlgorithm implements Algorithm {

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"../", "..\\"};

    private String[] webInfos= new String[]{"WEB-INF", "web.xml"};

    private Integer fileInitAction = 0;

    private RaspConfig raspConfig;

    private final RaspLog logger;

    private String metaInfo;

    public FileInitAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.travelStr = ParamSupported.getParameter(configMaps, "travel_str", String[].class, travelStr);
        this.webInfos = ParamSupported.getParameter(configMaps, "web_info", String[].class, webInfos);
        this.fileInitAction = ParamSupported.getParameter(configMaps, "file_init_action", Integer.class, fileInitAction);
    }

    public FileInitAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    @Override
    public String getType() {
        return "file-init";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        if (fileInitAction >= 0) {

            if (parameters != null) {
                String pathname = (String) parameters[0];
                if (StringUtils.isNotBlank(pathname)) {

                    // 算法1：路径穿越
                    for (String item : travelStr) {
                        if (pathname.contains(item)) {
                            boolean enableBlock = fileInitAction == 1;
                            AttackInfo attackInfo = new AttackInfo(context, metaInfo, pathname, enableBlock,
                                    "路径穿越", "路径穿越检测算法", "pathname: " + pathname, 80);
                            logger.attack(attackInfo);
                            if (enableBlock) {
                                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("travel file block by RASP."));
                            }
                        }
                    }

                    // 算法2：WEB敏感目录访问
                    for (String item : webInfos) {
                        if (pathname.contains(item)) {
                            boolean enableBlock = fileInitAction == 1;
                            AttackInfo attackInfo = new AttackInfo(context, metaInfo, pathname, enableBlock,
                                    "敏感信息", "敏感信息获取", "pathname: " + pathname, 80);
                            logger.attack(attackInfo);
                            if (enableBlock) {
                                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("web sensitive file block by RASP."));
                            }
                        }
                    }
                }
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
        return "file init with travel string";
    }
}
