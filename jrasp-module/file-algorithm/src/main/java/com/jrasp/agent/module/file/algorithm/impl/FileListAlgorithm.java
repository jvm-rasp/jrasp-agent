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
import java.util.*;

/**
 * @author jrasp
 * 路径遍历
 */
public class FileListAlgorithm implements Algorithm {

    /**
     * 兼容 windows、unix
     */
    private String[] travelStr = new String[]{"..", "../", "..\\"};

    private Integer fileListAction = 0;

    private RaspConfig raspConfig;

    private final RaspLog logger;

    private String metaInfo;

    /**
     * 系统根路径下主要目录
     */
    private Set<String> dangerDirList = new HashSet<String>(Arrays.asList(
            "/", "/home", "/etc",
            "/usr", "/usr/local",
            "/var/log", "/proc",
            "/sys", "/root",
            "C:\\", "D:\\", "E:\\", "F:\\", "G:\\", "/opt")
    );

    private List<String> jspShellList = new ArrayList<String>(Arrays.asList(
            "equals(FileOperation.java)",
            "filemanager.Dir.equals",
            "net.rebeyond.behinder",
            "(payload.java)"
    ));

    public FileListAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
        this.travelStr = ParamSupported.getParameter(configMaps, "travel_str", String[].class, travelStr);
        this.fileListAction = ParamSupported.getParameter(configMaps, "file_list_action", Integer.class, fileListAction);
        this.jspShellList = ParamSupported.getParameter(configMaps, "jsp_shell_list", List.class, jspShellList);
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

                if (isWhiteList(context)) {
                    return;
                }

                if (isBehinderFileOperationBackdoor()) {
                    doActionCtl(1, context, path, "Behinder FileOperation Backdoor, list file with travel path", "realpath:" + realpath, 100);
                }

                // jsp webshell stack 检测算法
                if (context != null && context.fromJsp()) {
                    String[] stackTraces = StackTrace.getStackTraceString(100, false);
                    for (String stack : stackTraces) {
                        for (String webshell : jspShellList) {
                            if (stack.contains(webshell)) {
                                doActionCtl(fileListAction, context, path, "list file with jsp shell", "realpath:" + realpath + ", jsp shell: " + webshell, 100);
                            }
                        }
                    }
                }


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
            AttackInfo attackInfo = new AttackInfo(context, metaInfo, path, enableBlock, "目录遍历", algorithm, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("list file block by JRASP."));
            }
        }
    }

    protected boolean isBehinderFileOperationBackdoor() {
        String[] stacks = StackTrace.getStackTraceString();
        boolean flag1, flag2 = false;
        for (int i = 0; i < stacks.length; i++) {
            flag1 = stacks[i].contains("list") && stacks[i].contains("FileOperation");
            if (stacks.length > i + 1) {
                flag2 = stacks[i + 1].contains("equals") && stacks[i + 1].contains("FileOperation");
            }
            if (flag1 && flag2) {
                return true;
            }
        }
        return false;
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
        return "list file with travel/root path";
    }

    public static List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<String>();
        // bugfix: 路径参数分割, 使用自定义分割符号
        StringTokenizer tokenizer = new StringTokenizer(str, " \t\n\r\f\\/");
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }

    public static String include(String httpParameters, List<String> tokens) {
        if (httpParameters != null) {
            for (String item : tokens) {
                if (httpParameters.contains(item)) {
                    return item;
                }
            }
        }
        return null;
    }
}
