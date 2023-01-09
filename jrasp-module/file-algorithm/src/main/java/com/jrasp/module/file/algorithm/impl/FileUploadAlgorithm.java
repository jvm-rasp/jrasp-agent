package com.jrasp.module.file.algorithm.impl;


import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class FileUploadAlgorithm implements Algorithm {

    private Integer fileUploadAction = 0;

    private final RaspLog logger;

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"../", "..\\"};

    //禁止上传脚本文件
    private String[] blackList = new String[]{".jsp", ".asp", ".phar", ".phtml", ".sh", ".py", ".pl", ".rb"};

    @Override
    public String getType() {
        return "file-upload";
    }

    public FileUploadAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public FileUploadAlgorithm(Map<String, String> configMaps, RaspLog logger) {
        this.logger = logger;
        this.travelStr = ParamSupported.getParameter(configMaps, "travelStr", String[].class, travelStr);
        this.fileUploadAction = ParamSupported.getParameter(configMaps, "fileUploadAction", Integer.class, fileUploadAction);
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (context != null && parameters != null) {
            File file = (File) parameters[0];
            String path = file.getPath();
            String realpath;
            try {
                realpath = file.getCanonicalPath();
            } catch (IOException e) {
                realpath = file.getAbsolutePath();
            }

            // 防护方式1：禁止木马进行任何文件行为
            String requestURL = context.getRequestURL();
            if (requestURL != null && requestURL.endsWith(".jsp")) {
                doActionCtl(fileUploadAction, context, path, "upload file in jsp", "realpath:" + realpath, 80);
                return;
            }

            // 防护方式2：禁止脚本文件的上传
            String s = path.toLowerCase();
            for (String item : blackList) {
                if (s.contains(item)) {
                    doActionCtl(fileUploadAction, context, path, "disable script file upload", "realpath: " + realpath, 50);
                }
            }

            //防护方式3：监控带有目录穿越的文件上传操作，后期可以切换为拦截。
            for (String item : travelStr) {
                if (path.contains(item)) {
                    doActionCtl(fileUploadAction, context, path, "upload file with travel path", "realpath:" + realpath, 80);
                    return;
                }
            }
        }


    }

    @Override
    public String getDescribe() {
        return null;
    }

    private void doActionCtl(int action, Context context, String payload, String algorithm, String message, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, payload, enableBlock, getType(), algorithm, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessControlException.throwThrowsImmediately(new RuntimeException("upload file block by rasp."));
            }
        }
    }
}
