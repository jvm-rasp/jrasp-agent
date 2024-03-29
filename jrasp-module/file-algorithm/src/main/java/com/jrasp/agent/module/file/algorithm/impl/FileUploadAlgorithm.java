package com.jrasp.agent.module.file.algorithm.impl;


import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StackTrace;
import com.jrasp.agent.api.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileUploadAlgorithm implements Algorithm {

    private Integer fileUploadAction = 0;

    private RaspConfig raspConfig;

    private final RaspLog logger;

    private String metaInfo;

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"../", "..\\"};

    //禁止上传脚本文件
    private String[] fileUploadBlackList = new String[]{
            ".jsp", "jspx", ".asp", ".phar", ".phtml", ".sh", ".py", ".pl", ".rb", ".exe", ".scr", ".vbs", ".cmd", ".bat",
            ".so", ".dll", ".ps1", "authorized_key"
    };

    private List<String> jspShellList = new ArrayList<String>(Arrays.asList(
            "equals(FileOperation.java)",
            "filemanager.Dir.equals",
            "net.rebeyond.behinder",
            "(payload.java)"
    ));

    @Override
    public String getType() {
        return "file-upload";
    }

    public FileUploadAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    public FileUploadAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.travelStr = ParamSupported.getParameter(configMaps, "travel_str", String[].class, travelStr);
        this.fileUploadAction = ParamSupported.getParameter(configMaps, "file_upload_action", Integer.class, fileUploadAction);
        this.fileUploadBlackList = ParamSupported.getParameter(configMaps, "file_upload_black_list", String[].class, fileUploadBlackList);
        this.jspShellList = ParamSupported.getParameter(configMaps, "jsp_shell_list", List.class, jspShellList);
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        if (context != null && parameters != null) {
            File file = (File) parameters[0];
            String path = file.getPath();
            String realpath;
            try {
                realpath = file.getCanonicalPath();
            } catch (IOException e) {
                realpath = file.getAbsolutePath();
            }

            // jsp webshell stack 检测算法
            if (context != null && context.fromJsp()) {
                String[] stackTraces = StackTrace.getStackTraceString(100, false);
                for (String stack : stackTraces) {
                    for (String webshell : jspShellList) {
                        if (stack.contains(webshell)) {
                            doActionCtl(fileUploadAction, context, path, "upload file with jsp shell", "realpath:" + realpath + ", jsp shell: " + webshell, 100);
                        }
                    }
                }
            }

            // 防护方式1：禁止木马进行任何文件行为
            String requestURL = context.getRequestURL();
            if (requestURL != null && requestURL.endsWith(".jsp")) {
                if (requestURL.contains("check") || requestURL.contains("ewebeditor") || requestURL.contains("ueditor")
                        || requestURL.contains("druid") || requestURL.contains("stimulsoftreport")) {
                    return;
                }
                doActionCtl(fileUploadAction, context, path, "upload file in jsp", "realpath:" + realpath, 80);
                return;
            }


            // 防护方式2：禁止脚本文件的上传
            String s = path.toLowerCase();
            for (String item : fileUploadBlackList) {
                if (s.contains(item)) {
                    doActionCtl(fileUploadAction, context, path, "disable script file upload", "realpath: " + realpath, 50);
                }
            }

            //防护方式3：监控带有目录穿越的文件上传操作，后期可以切换为拦截。
            for (String item : travelStr) {
                if (path.contains("../logs")) {
                    return;
                }
                if (path.contains(item)) {
                    doActionCtl(fileUploadAction, context, path, "upload file with travel path", "realpath:" + realpath, 80);
                    return;
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
        return null;
    }

    private void doActionCtl(int action, Context context, String payload, String algorithm, String message, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, metaInfo, payload, enableBlock, "任意文件上传", algorithm, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("upload file block by JRASP."));
            }
        }
    }
}
