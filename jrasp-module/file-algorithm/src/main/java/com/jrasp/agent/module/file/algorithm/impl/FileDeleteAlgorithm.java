package com.jrasp.agent.module.file.algorithm.impl;

import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author jrasp
 * 路径穿越文件删除
 */
public class FileDeleteAlgorithm implements Algorithm {

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"../", "..\\"};

    private Integer fileDeleteAction = 0;

    private RaspConfig raspConfig;

    private final RaspLog logger;

    private String metaInfo;

    public FileDeleteAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.travelStr = ParamSupported.getParameter(configMaps, "travel_str", String[].class, travelStr);
        this.fileDeleteAction = ParamSupported.getParameter(configMaps, "file_delete_action", Integer.class, fileDeleteAction);
    }

    public FileDeleteAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    @Override
    public String getType() {
        return "file-delete";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        if (fileDeleteAction >= 0) {
            File file = (File) parameters[0];
            String path = file.getPath();
            String realpath;
            try {
                realpath = file.getCanonicalPath();
            } catch (IOException e) {
                realpath = file.getAbsolutePath();
            }

            if (StringUtils.isNotBlank(realpath)) {
                for (String item : travelStr) {
                    if (path.contains(item)) {
                        boolean enableBlock = fileDeleteAction == 1;
                        AttackInfo attackInfo = new AttackInfo(context, metaInfo, path, enableBlock,
                                "任意文件删除", getDescribe(), "realpath: " + realpath, 80);
                        logger.attack(attackInfo);
                        if (enableBlock) {
                            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("delete file block by EpointRASP."));
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
        return "delete file with travel string";
    }
}
