package com.jrasp.agent.api;

/**
 * 全局配置
 */
public interface RaspConfig {

    boolean isCheckDisable();

    void setCheckDisable(boolean checkEnable);

    String getRedirectUrl();

    String getJsonBlockContent();

    String getXmlBlockContent();

    String getHtmlBlockContent();

    void setRedirectUrl(String redirectUrl);

    void setJsonBlockContent(String jsonBlockContent);

    void setXmlBlockContent(String xmlBlockContent);

    void setHtmlBlockContent(String htmlBlockContent);

    int getBlockStatusCode();

    void setBlockStatusCode(int blockStatusCode);

}
