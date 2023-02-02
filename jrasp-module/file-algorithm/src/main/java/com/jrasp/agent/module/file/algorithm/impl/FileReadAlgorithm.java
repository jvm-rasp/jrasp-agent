package com.jrasp.agent.module.file.algorithm.impl;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author jrasp
 * 文件读取
 */
public class FileReadAlgorithm implements Algorithm {

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"../", "..\\"};

    private Integer fileReadAction = 0;

    private final RaspLog logger;

    /**
     * 系统根路径下主要目录
     */
    private Set<String> dangerDirList = new HashSet<String>(Arrays.asList("/", "/home", "/etc", "/usr", "/usr/local", "/var/log", "/proc", "/sys", "/root", "C:\\", "D:\\", "E:\\"));

    public FileReadAlgorithm(Map<String, String> configMaps, RaspLog logger) {
        this.logger = logger;
        this.travelStr = ParamSupported.getParameter(configMaps, "travel_str", String[].class, travelStr);
        this.fileReadAction = ParamSupported.getParameter(configMaps, "file_read_action", Integer.class, fileReadAction);
        this.dangerDirList = ParamSupported.getParameter(configMaps, "danger_dir_list", Set.class, dangerDirList);
    }

    public FileReadAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    @Override
    public String getType() {
        return "file-read";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (fileReadAction >= 0 && parameters != null && context != null) {
            File file = (File) parameters[0];
            String path = file.getPath();
            String realpath;
            try {
                realpath = file.getCanonicalPath();
            } catch (IOException e) {
                realpath = file.getAbsolutePath();
            }

            if (StringUtils.isBlank(path)) {
                return;
            }

            // 算法1: 简单用户输入识别，拦截任意文件下载漏洞
            List<String> tokens = getTokens(path);
            String includeParameter = include(context.getParametersString(), tokens);
            if (includeParameter != null) {
                doActionCtl(fileReadAction, context, path, "read file token contains in parameters", includeParameter, 80);
                return;
            }
            String includeHeader = include(context.getHeaderString(), tokens);
            if (includeHeader != null) {
                doActionCtl(fileReadAction, context, path, "read file contains in headers", includeHeader, 80);
                return;
            }

            // 算法2：请求来源于jsp, 禁止读取文件
            String requestURL = context.getRequestURL();
            if (requestURL != null && requestURL.endsWith(".jsp")) {
                doActionCtl(fileReadAction, context, path, "read file in jsp", "realpath:" + realpath, 80);
                return;
            }

            // 算法3：任意路径文件读取
            for (String item : travelStr) {
                if (path.contains(item)) {
                    doActionCtl(fileReadAction, context, path, "read file with travel path", "realpath:" + realpath, 80);
                    return;
                }
            }

            // 算法4：系统根路径下子目录的遍历
            if (dangerDirList.contains(realpath)) {
                doActionCtl(fileReadAction, context, path, "read file is root path", "realpath:" + realpath, 80);
                return;
            }
            // 其他算法添加到下面

        }
    }

    private void doActionCtl(int action, Context context, String path, String checkType, String message, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, path, enableBlock, getType(), checkType, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessControlException.throwThrowsImmediately(new RuntimeException("read file block by rasp."));
            }
        }
    }

    @Override
    public String getDescribe() {
        return "list file with travel/root path";
    }

    private String include(String httpParameters, List<String> cmdArgs) {
        if (httpParameters != null) {
            for (String item : cmdArgs) {
                if (httpParameters.contains(item)) {
                    return item;
                }
            }
        }
        return null;
    }

    public static List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(str);
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }
}
