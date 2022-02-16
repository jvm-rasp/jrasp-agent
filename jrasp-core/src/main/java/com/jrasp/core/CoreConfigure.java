package com.jrasp.core;

import com.jrasp.api.Information;
import com.jrasp.core.util.ProcessHelper;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

// 内核启动配置
public class CoreConfigure {

    // ----------------------------------------------- AgentLauncher传递过来的值------------------------------------------
    private static final String KEY_RASP_HOME = "raspHome";
    private static final String KEY_NAMESPACE = "namespace";
    private static final String KEY_LAUNCH_MODE = "mode";
    private static final String VAL_LAUNCH_MODE_AGENT = "agent";
    private static final String VAL_LAUNCH_MODE_ATTACH = "attach";
    private static final String KEY_SERVER_IP = "serverIp";
    private static final String KEY_SERVER_PORT = "serverPort";

    //------------------------------------------------配置参数-----------------------------------------------------------

    // 默认登陆用户名
    private static final String KEY_USERNAME = "username";
    private static final String DEFAULT_VAL_USERNAME = "admin";

    // 默认登陆密码
    private static final String KEY_PASSWORD = "password";
    private static final String DEFAULT_VAL_PASSWORD = "123456";

    // module解密密钥
    private static final String KEY_ENCRYPTION = "encryption";
    private static final String DEFAULT_VAL_ENCRYPTION = ""; //默认为空，不加密

    // jrasp技术支持链接
    private static final String KEY_SUPPORT_URL = "supportURL";
    private static final String DEFAULT_SUPPORT_URL = "technical support：http://www.jrasp.com/";

    // 是否开启鉴权
    private static final String KEY_ENBALE_AUTH = "enableAuth";

    //------------------------------------------------常量参数-----------------------------------------------------------
    private static final String SYSTEM_MODULE_FILE_NAME = "system-module";

    private static final String REQUIRED_MODULE_FILE_NAME = "required-module";

    private static final String ALGORITHM_MODULE_FILE_NAME = "algorithm-module";

    private static final String OPTIONAL_MODULE_FILE_NAME = "optional-module";

    // 初始化参数文件
    private static final String TOKEN_FILE_NAME = ".jrasp.token";

    private final Map<String, String> featureMap = new LinkedHashMap<String, String>();

    private CoreConfigure(final Map<String, String> featureMap) {
        this.featureMap.putAll(featureMap);
    }

    private static volatile CoreConfigure instance;

    // 被agent反射调用
    public static CoreConfigure toConfigure(final Map<String, String> featureMap) {
        return instance = new CoreConfigure(featureMap);
    }

    public static CoreConfigure getInstance() {
        return instance;
    }

    // 获取容器的命名空间
    public String getNamespace() {
        String namespace = featureMap.get(KEY_NAMESPACE);
        assert StringUtils.isNotBlank(namespace);
        return namespace;
    }

    // 获取沙箱安装目录
    public String getRaspHome() {
        return featureMap.get(KEY_RASP_HOME);
    }

    // 获取配置文件加载路径
    public String getCfgLibPath() {
        return getRaspHome() + File.separatorChar + "cfg";
    }

    // 获取日志文件路径
    public String getLogsPath() {
        return getRaspHome() + File.separatorChar + "logs";
    }

    // 获取运行时文件路径
    public String getProcessRunPath() {
        return getRaspHome() + File.separatorChar + "run";
    }

    // 获取临时文件路径
    public String getTempPath() {
        return getRaspHome() + File.separatorChar + "temp";
    }

    // 获取系统模块加载路径(文件变化的监控路径)
    public String getSystemModuleLibPath() {
        return getRaspHome() + File.separatorChar + SYSTEM_MODULE_FILE_NAME;
    }

    // 获取用户模块(必装)模块路径(文件变化的监控路径)
    public String getAlgorithmModuleLibPath() {
        return getRaspHome() + File.separatorChar + ALGORITHM_MODULE_FILE_NAME;
    }

    // 获取用户模块(必装)模块路径(文件变化的监控路径)
    public String getUserModuleLibPath() {
        return getRaspHome() + File.separatorChar + REQUIRED_MODULE_FILE_NAME;
    }

    // 获取用户模块(可选)模块路径(文件仓库) todo 暂无用处
    public String getUserModuleLibPath2() {
        return getRaspHome() + File.separatorChar + OPTIONAL_MODULE_FILE_NAME;
    }

    // 获取沙箱内部服务提供库目录(文件变化的监控路径)
    public String getProviderLibPath() {
        return getRaspHome() + File.separatorChar + "provider";
    }

    // 是否以Agent模式启动
    private boolean isLaunchByAgentMode() {
        return StringUtils.equals(featureMap.get(KEY_LAUNCH_MODE), VAL_LAUNCH_MODE_AGENT);
    }

    // 是否以Attach模式启动
    private boolean isLaunchByAttachMode() {
        return StringUtils.equals(featureMap.get(KEY_LAUNCH_MODE), VAL_LAUNCH_MODE_ATTACH);
    }

    // 获取沙箱的启动模式
    public Information.Mode getLaunchMode() {
        if (isLaunchByAgentMode()) {
            return Information.Mode.AGENT;
        }
        if (isLaunchByAttachMode()) {
            return Information.Mode.ATTACH;
        }
        return Information.Mode.ATTACH;
    }

    // 是否启用Unsafe功能
    public boolean isEnableUnsafe() {
        return true; // 大部分情况是true
    }

    // 获取服务器绑定IP
    public String getServerIp() {
        return StringUtils.isNotBlank(featureMap.get(KEY_SERVER_IP))
                ? featureMap.get(KEY_SERVER_IP)
                : "127.0.0.1";
    }

    // 获取是否开启http请求鉴权开关值 todo 验证功能
    public boolean getEnableAuth() {
        String isEnableAuth = featureMap.get(KEY_ENBALE_AUTH);
        return StringUtils.isNotBlank(isEnableAuth) && BooleanUtils.toBoolean(isEnableAuth);
    }

    // 获取服务器端口
    public int getServerPort() {
        return NumberUtils.toInt(featureMap.get(KEY_SERVER_PORT), 0);
    }

    //  获取服务器编码
    public Charset getServerCharset() {
        try {
            return Charset.forName("UTF-8");
        } catch (Exception cause) {
            return Charset.defaultCharset();
        }
    }

    // 获取技术支持URL
    public String getSupportURL() {
        String keySupportUrl = featureMap.get(KEY_SUPPORT_URL);
        return StringUtils.isNotBlank(keySupportUrl)
                ? keySupportUrl
                : DEFAULT_SUPPORT_URL;
    }

    // 获取加密密钥
    public String getEncryption() {
        String encryption = featureMap.get(KEY_ENCRYPTION);
        return StringUtils.isNotBlank(encryption)
                ? encryption
                : DEFAULT_VAL_ENCRYPTION;
    }

    // 获取登陆用户名称
    public String getUsername() {
        String username = featureMap.get(KEY_USERNAME);
        return StringUtils.isNotBlank(username)
                ? username
                : DEFAULT_VAL_USERNAME;
    }

    // 获取登陆用户密码
    public String getPassword() {
        String password = featureMap.get(KEY_PASSWORD);
        return StringUtils.isNotBlank(password)
                ? password
                : DEFAULT_VAL_PASSWORD;
    }

    // 设置登陆用户名称
    public void setUsername(String username) {
        featureMap.put(KEY_USERNAME, username);
    }

    // 设置登陆用户密码
    public void setPassword(String password) {
        featureMap.put(KEY_PASSWORD, password);
    }

    // 设置module解密密码
    public void setEncryption(String encryption) {
        featureMap.put(KEY_ENCRYPTION, encryption);
    }

    // 获取进程运行时pid目录
    public String getProcessPidPath() {
        return getProcessRunPath() + File.separator + ProcessHelper.getCurrentPID();
    }

    // 获取进程运行时pid/system-module
    public String getRuntimeSystemModulePath() {
        return getProcessPidPath() + File.separator + SYSTEM_MODULE_FILE_NAME;
    }

    // 获取进程运行时pid/required-module
    public String getRuntimeRequiredModulePath() {
        return getProcessPidPath() + File.separator + REQUIRED_MODULE_FILE_NAME;
    }

    // 获取进程运行时pid/algorithm-module
    public String getRuntimeAlgorithmModulePath() {
        return getProcessPidPath() + File.separator + ALGORITHM_MODULE_FILE_NAME;
    }

    // 获取进程运行时pid/optional-nodule
    public String getRuntimeOptionalModulePath() {
        return getProcessPidPath() + File.separator + OPTIONAL_MODULE_FILE_NAME;
    }

    //  获取进程运行时pid/
    public String getRuntimeTokenPath() {
        return getProcessPidPath() + File.separatorChar + TOKEN_FILE_NAME;
    }

}
