package com.jrasp.agent.module.file.algorithm.impl;

import com.epoint.core.utils.classpath.ClassPathUtil;
import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;
import com.jrasp.agent.module.file.algorithm.util.FileCheck;
import com.jrasp.agent.module.file.algorithm.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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

    private RaspConfig raspConfig;

    private final RaspLog logger;

    private String metaInfo;

    /**
     * 系统根路径下主要目录
     */
    private Set<String> dangerDirList = new HashSet<String>(Arrays.asList("/", "/home", "/etc", "/usr", "/usr/local", "/var/log", "/proc", "/sys", "/root", "C:\\", "D:\\", "E:\\"));

    private static final Pattern readFileWhiteExt = Pattern.compile("\\.(do[ct][xm]?|xl[s|t][xmb]?|pp[tsa][xm]?|pot[xm]|7z|tar|gz|bz2|xz|rar|zip|jpg|jpeg|png|gif|bmp|txt|lic|tmp|htm|html)$", Pattern.CASE_INSENSITIVE);

    public FileReadAlgorithm(Map<String, String> configMaps, RaspConfig raspConfig, RaspLog logger, String metaInfo) {
        this.logger = logger;
        this.raspConfig = raspConfig;
        this.metaInfo = metaInfo;
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
        if (isWhiteList(context)) {
            return;
        }
        if (fileReadAction >= 0 && parameters != null && context != null) {
            File file = (File) parameters[0];
            String path = file.getPath();
            String pth;
            try {
                pth = file.getCanonicalPath();
            } catch (IOException e) {
                pth = file.getAbsolutePath();
            }

            if (pth.endsWith(".class") || pth.endsWith(".jar") || pth.endsWith(".war")) {
                return;
            }

            String realpath = FileUtil.getRealPath(file);

            if (StringUtils.isBlank(path)) {
                return;
            }

            String[] pathParts = path.split("://");
            String proto = "";
            if (pathParts.length > 1) {
                proto = pathParts[0].toLowerCase();
            }

            List<String> params = new ArrayList<String>();
            if (context.getDecryptParameters() != null) {
                for (Map.Entry<String, String[]> entry : context.getDecryptParameters().entrySet()) {
                    params.addAll(Arrays.asList(entry.getValue()));
                }
            }
            if (context.getHeader() != null) {
                for (Map.Entry<String, String> entry : context.getHeader().entrySet()) {
                    params.add(entry.getValue());
                }
            }

            String[] allParams = params.toArray(new String[0]);

            // 算法1：检测读取的文件路径是否从请求参数中传入
            if ((proto.equals("") || proto.equals("file")) && !readFileWhiteExt.matcher(realpath).find() && FileCheck.isPathEndWithUserInput(allParams, path, realpath, false)) {
                doActionCtl(fileReadAction, context, path, "read file token contains in parameters", path, 80);
                return;
            }

            if (FileCheck.isFromUserInput(allParams, path) && proto.equals("file")) {
                doActionCtl(fileReadAction, context, path, "read file token contains in parameters", String.format("path: [%s], proto: [%s]", path, proto), 80);
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
            AttackInfo attackInfo = new AttackInfo(context, ClassPathUtil.getWebContext(), metaInfo, path, enableBlock, "任意文件读取", checkType, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("read file block by EpointRASP."));
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
