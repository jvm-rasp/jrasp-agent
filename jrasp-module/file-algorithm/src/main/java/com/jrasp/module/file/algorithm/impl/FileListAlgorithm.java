package com.jrasp.module.file.algorithm.impl;

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
 * 路径遍历
 */
public class FileListAlgorithm implements Algorithm {

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"../", "..\\"};

    private Integer fileListAction = 0;

    private final RaspLog logger;

    /**
     * 系统根路径下主要目录
     */
    private Set<String> dangerDirList = new HashSet<String>(Arrays.asList(
            "/", "/home", "/etc",
            "/usr", "/usr/local",
            "/var/log", "/proc",
            "/sys", "/root",
            "C:\\", "D:\\", "E:\\")
    );

    public FileListAlgorithm(Map<String, String> configMaps, RaspLog logger) {
        this.logger = logger;
        this.travelStr = ParamSupported.getParameter(configMaps, "travel_str", String[].class, travelStr);
        this.fileListAction = ParamSupported.getParameter(configMaps, "file_list_action", Integer.class, fileListAction);
    }

    public FileListAlgorithm(RaspLog logger) {
        this.logger = logger;
    }

    @Override
    public String getType() {
        return "file-list";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (fileListAction >= 0) {
            File file = (File) parameters[0];
            String path = file.getPath();
            String realpath;
            try {
                realpath = file.getCanonicalPath();
            } catch (IOException e) {
                realpath = file.getAbsolutePath();
            }

            if (StringUtils.isNotBlank(path)) {

                //  算法1：路径穿越检测算法
                for (String item : travelStr) {
                    if (path.contains(item)) {
                        doActionCtl(fileListAction, context, path, "list file with travel path", "realpath:" + realpath, 80);
                        return;
                    }
                }

                // 算法2：系统根路径下子目录的遍历
                if (dangerDirList.contains(realpath)) {
                    doActionCtl(fileListAction, context, path, "list file with root path", "realpath: " + realpath, 80);
                    return;
                }

                // 算法3: 用户输入匹配
                List<String> tokens = getTokens(path);
                String includeParameter = include(context.getParametersString(), tokens);
                if (includeParameter != null) {
                    doActionCtl(fileListAction, context, path, "list file contains in parameters", includeParameter, 80);
                    return;
                }
                String includeHeader = include(context.getHeaderString(), tokens);
                if (includeHeader != null) {
                    doActionCtl(fileListAction, context, path, "list file contains in headers", includeHeader, 80);
                    return;
                }
                // todo 其他算法这里添加

            }
        }
    }

    private void doActionCtl(int action, Context context, String path, String algorithm, String message, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, path, enableBlock, getType(), algorithm, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessControlException.throwThrowsImmediately(new RuntimeException("list file block by rasp."));
            }
        }
    }


    @Override
    public String getDescribe() {
        return "list file with travel/root path";
    }

    public static List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(str);
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
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
}
