package com.jrasp.module.file.algorithm.impl;

import com.jrasp.agent.api.ProcessController;
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

    private Integer action = 0;

    private final RaspLog logger;

    public FileDeleteAlgorithm(Map<String, String> configMaps, RaspLog logger) {
        this.logger = logger;
        this.travelStr = ParamSupported.getParameter(configMaps, "travelStr", String[].class, travelStr);
        this.action = ParamSupported.getParameter(configMaps, "action", Integer.class, action);
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
        if (action >= 0) {
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
                        boolean enableBlock = action == 1;
                        AttackInfo attackInfo = new AttackInfo(context, path, enableBlock,
                                getType(), getDescribe(), "realpath: " + realpath, 80);
                        logger.attack(attackInfo);
                        if (enableBlock) {
                            ProcessController.throwsImmediately(new RuntimeException("delete file block by rasp."));
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "delete file with travel string";
    }
}
