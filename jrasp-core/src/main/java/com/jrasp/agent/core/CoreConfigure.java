package com.jrasp.agent.core;

import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.core.util.FeatureCodec;
import com.jrasp.agent.core.util.ProcessHelper;
import com.jrasp.agent.core.util.number.NumberUtils;
import com.jrasp.agent.core.util.string.RaspStringUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 内核启动配置
 * Created by luanjia@taobao.com on 16/10/2.
 */
public class CoreConfigure implements RaspConfig {

    private static final String KEY_NAMESPACE = "namespace";
    private static final String DEFAULT_VAL_NAMESPACE = "default";

    private static final String KEY_SANDBOX_HOME = "home";
    private static final String KEY_LAUNCH_MODE = "mode";
    private static final String KEY_SERVER_IP = "server.ip";
    private static final String KEY_SERVER_PORT = "server.port";

    private static final String VAL_LAUNCH_MODE_AGENT = "agent";
    private static final String VAL_LAUNCH_MODE_ATTACH = "attach";

    private static final String KEY_LOGS_LIB_PATH = "logs";

    private static final String KEY_MODULE_LIB_PATH = "module";

    // 初始化参数文件
    private static final String TOKEN_FILE_NAME = ".jrasp.token";

    // 技术支持链接
    public static final String JRASP_SUPPORT_URL = "https://www.jrasp.com";

    // go viper 不区分大小写, java为了便于区分，这里使用下划线命名规则
    private volatile int block_status_code = 302;

    private volatile boolean check_disable = false;

    private volatile String redirect_url = "https://www.jrasp.com/block.html";

    private volatile String json_block_content = "{\"error\":true, \"reason\": \"Request blocked by JRASP (https://www.jrasp.com)\"}";

    private volatile String xml_block_content = "<?xml version=\"1.0\"?><doc><error>true</error><reason>Request blocked by JRASP</reason></doc>";

    private volatile String html_block_content = "</script><script>location.href=\"https://www.jrasp.com/block.html\"</script>";

    private static final FeatureCodec codec = new FeatureCodec(';', '=');

    private final Map<String, String> featureMap = new LinkedHashMap<String, String>();

    private CoreConfigure(final String featureString) {
        final Map<String, String> featureMap = toFeatureMap(featureString);
        this.featureMap.putAll(featureMap);
    }

    private Map<String, String> toFeatureMap(String featureString) {
        return codec.toMap(featureString);
    }

    private static volatile CoreConfigure instance;

    public static CoreConfigure toConfigure(final String featureString) {
        return instance = new CoreConfigure(featureString);
    }

    public static CoreConfigure getInstance() {
        return instance;
    }

    /**
     * 获取容器的命名空间
     *
     * @return 容器的命名空间
     */
    public String getNamespace() {
        final String namespace = featureMap.get(KEY_NAMESPACE);
        return RaspStringUtils.isNotBlank(namespace)
                ? namespace
                : DEFAULT_VAL_NAMESPACE;
    }

    @Override
    public String toString() {
        return codec.toString(featureMap);
    }

    /**
     * 是否以Agent模式启动
     *
     * @return true/false
     */
    private boolean isLaunchByAgentMode() {
        return RaspStringUtils.equals(featureMap.get(KEY_LAUNCH_MODE), VAL_LAUNCH_MODE_AGENT);
    }

    /**
     * 是否以Attach模式启动
     *
     * @return true/false
     */
    private boolean isLaunchByAttachMode() {
        return RaspStringUtils.equals(featureMap.get(KEY_LAUNCH_MODE), VAL_LAUNCH_MODE_ATTACH);
    }

    /**
     * 获取沙箱的启动模式
     * 默认按照ATTACH模式启动
     *
     * @return 沙箱的启动模式
     */
    public Information.Mode getLaunchMode() {
        if (isLaunchByAgentMode()) {
            return Information.Mode.AGENT;
        }
        if (isLaunchByAttachMode()) {
            return Information.Mode.ATTACH;
        }
        return Information.Mode.ATTACH;
    }

    /**
     * 是否启用Unsafe功能
     *
     * @return unsafe.enable
     */
    public boolean isEnableUnsafe() {
        return true;
    }

    /**
     * 获取沙箱安装目录
     *
     * @return 沙箱安装目录
     */
    public String getJvmSandboxHome() {
        return featureMap.get(KEY_SANDBOX_HOME);
    }

    /**
     * 获取服务器绑定IP
     *
     * @return 服务器绑定IP
     */
    public String getServerIp() {
        return RaspStringUtils.isNotBlank(featureMap.get(KEY_SERVER_IP))
                ? featureMap.get(KEY_SERVER_IP)
                : "127.0.0.1";
    }

    /**
     * 获取服务器端口
     *
     * @return 服务器端口
     */
    public int getServerPort() {
        return NumberUtils.toInt(featureMap.get(KEY_SERVER_PORT), 0);
    }

    // 获取运行时文件路径
    public String getProcessRunPath() {
        return getJvmSandboxHome() + File.separatorChar + "run";
    }

    public String getLogsPath() {
        return getJvmSandboxHome() + File.separator + KEY_LOGS_LIB_PATH;
    }

    // 获取进程运行时pid目录
    public String getProcessPidPath() {
        return getProcessRunPath() + File.separator + ProcessHelper.getCurrentPID();
    }

    // 获取进程运行时pid目录
    public String getRunModulePath() {
        return getProcessPidPath() + File.separator + KEY_MODULE_LIB_PATH;
    }

    public String getModuleLibPath() {
        return getJvmSandboxHome() + File.separator + KEY_MODULE_LIB_PATH;
    }

    /**
     * 获取用户模块加载文件/目录(集合)
     *
     * @return 用户模块加载文件/目录(集合)
     */
    public synchronized File[] getModuleLibFiles() {
        final Collection<File> foundModuleJarFiles = new LinkedHashSet<File>();
        String path = getModuleLibPath();
        final File fileOfPath = new File(path);
        if (fileOfPath.isDirectory()) {
            foundModuleJarFiles.addAll(FileUtils.listFiles(new File(path), new String[]{"jar"}, false));
        }
        return foundModuleJarFiles.toArray(new File[]{});
    }

    //  获取进程运行时pid/
    public String getRuntimeTokenPath() {
        return getProcessPidPath() + File.separatorChar + TOKEN_FILE_NAME;
    }

    @Override
    public boolean isCheckDisable() {
        return check_disable;
    }

    @Override
    public void setCheckDisable(boolean checkDisable) {
        this.check_disable = checkDisable;
    }

    @Override
    public String getRedirectUrl() {
        return this.redirect_url;
    }

    @Override
    public String getJsonBlockContent() {
        return json_block_content;
    }

    @Override
    public String getXmlBlockContent() {
        return xml_block_content;
    }

    @Override
    public String getHtmlBlockContent() {
        return html_block_content;
    }

    @Override
    public void setRedirectUrl(String redirectUrl) {
        this.redirect_url = redirectUrl;
    }

    @Override
    public void setJsonBlockContent(String jsonBlockContent) {
        this.json_block_content = jsonBlockContent;
    }

    @Override
    public void setXmlBlockContent(String xmlBlockContent) {
        this.xml_block_content = xmlBlockContent;
    }

    @Override
    public void setHtmlBlockContent(String htmlBlockContent) {
        this.html_block_content = htmlBlockContent;
    }

    @Override
    public int getBlockStatusCode() {
        return block_status_code;
    }

    @Override
    public void setBlockStatusCode(int blockStatusCode) {
        this.block_status_code = blockStatusCode;
    }

}
