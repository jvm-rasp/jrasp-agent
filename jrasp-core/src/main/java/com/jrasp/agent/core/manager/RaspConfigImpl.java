package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.RaspConfig;

/**
 * 模块全局配置
 * go viper 不区分大小写,无法使用驼峰命名规范
 * java为了便于区分，这里使用下划线命名规则
 */
public class RaspConfigImpl implements RaspConfig {

    private RaspConfigImpl() {
    }

    private static volatile RaspConfig instance = null;

    public static RaspConfig getInstance() {
        if (instance == null) {
            synchronized (RaspConfigImpl.class) {
                if (instance == null) {
                    instance = new RaspConfigImpl();
                }
            }
        }
        return instance;
    }

    private volatile int block_status_code = 302;

    private volatile boolean check_disable = false;

    private volatile String redirect_url = "https://www.jrasp.com/block.html";

    private volatile String json_block_content = "{\"error\":true, \"reason\": \"Request blocked by JRASP (https://www.jrasp.com)\"}";

    private volatile String xml_block_content = "<?xml version=\"1.0\"?><doc><error>true</error><reason>Request blocked by JRASP</reason></doc>";

    private volatile String html_block_content = "</script><script>location.href=\"https://www.jrasp.com/block.html\"</script>";

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
