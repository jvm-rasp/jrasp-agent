package com.jrasp.agent.api.request;

import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.util.Reflection;

/**
 * Created by tyy on 9/5/17.
 * javax.servlet.http.HttpServletResponse 类型响应的统一接口
 * from open-rasp 有改动
 */
public class HttpServletResponse {

    public static final String CONTENT_LENGTH_HEADER_KEY = "Content-Length";
    public static final String CONTENT_TYPE_HTML_VALUE = "text/html";
    public static final String CONTENT_TYPE_JSON_VALUE = "application/json";
    public static final String CONTENT_TYPE_XML_VALUE = "application/xml";
    public static final String CONTENT_TYPE_TEXT_XML = "text/xml";
    private Object response;

    /**
     * constructor
     *
     * @param response http响应实体
     */
    public HttpServletResponse(Object response) {
        this.response = response;
    }

    /**
     * 获取http相应实体
     *
     * @return http响应实体
     */
    public Object getResponse() {
        return response;
    }

    /**
     * 设置响应头,覆盖原值
     *
     * @param key   响应头名称
     * @param value 响应头值
     */
    public void setHeader(String key, String value) throws Exception {
        if (response != null) {
            Reflection.invokeMethod(response, "setHeader", new Class[]{String.class, String.class}, key, value);
        }
    }

    /**
     * 设置数字响应头,覆盖原值
     *
     * @param key   响应头名称
     * @param value 响应头值
     */
    public void setIntHeader(String key, int value) throws Exception {
        if (response != null) {
            Reflection.invokeMethod(response, "setIntHeader", new Class[]{String.class, int.class}, key, value);
        }
    }

    /**
     * 设置响应头，不覆盖
     *
     * @param key   响应头名称
     * @param value 响应头值
     */
    public void addHeader(String key, String value) throws Exception {
        if (response != null) {
            Reflection.invokeMethod(response, "addHeader", new Class[]{String.class, String.class}, key, value);
        }
    }

    /**
     * 获取响应头
     *
     * @param key 响应头名称
     * @return 响应头值值
     */
    public String getHeader(String key) throws Exception {
        if (response != null) {
            Object header = Reflection.invokeMethod(response, "getHeader", new Class[]{String.class}, key);
            if (header != null) {
                return header.toString();
            }
        }
        return null;
    }

    public String getCharacterEncoding() throws Exception {
        if (response != null) {
            Object enc = Reflection.invokeMethod(response, "getCharacterEncoding", new Class[]{});
            if (enc != null) {
                return enc.toString();
            }
        }
        return null;
    }

    public String getContentType() throws Exception {
        if (response != null) {
            Object contentType = Reflection.invokeMethod(response, "getContentType", new Class[]{});
            if (contentType != null) {
                return contentType.toString();
            }
        }
        return null;
    }

    /**
     * 清除所有 body buffer 缓存
     *
     * @return 是否成功
     */
    public boolean resetBuffer() {
        if (response != null) {
            try {
                Reflection.invokeMethod(response, "resetBuffer", new Class[]{});
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 清除所有 buffer 缓存
     *
     * @return 是否成功
     */
    public boolean reset() {
        if (response != null) {
            try {
                Reflection.invokeMethod(response, "reset", new Class[]{});
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    // 错误处理
    public void sendError(Context context, RaspConfig raspConfig) throws Exception {
        if (context != null) {
            Object response = context.getResponse();
            if (response != null) {
                int statusCode = raspConfig.getBlockStatusCode();
                String blockUrl = raspConfig.getRedirectUrl();
                boolean isCommitted = (Boolean) Reflection.invokeMethod(response, "isCommitted", new Class[]{});
                String contentType = context.getResponseContentType();
                String script;
                if (contentType != null && contentType.contains(CONTENT_TYPE_JSON_VALUE)) {
                    script = raspConfig.getJsonBlockContent();
                } else if (contentType != null && (contentType.contains(CONTENT_TYPE_XML_VALUE) || contentType.contains(CONTENT_TYPE_TEXT_XML))) {
                    script = raspConfig.getXmlBlockContent();
                } else {
                    script = raspConfig.getHtmlBlockContent();
                }

                if (!isCommitted) {
                    resetBuffer();
                    Reflection.invokeMethod(response, "setStatus", new Class[]{int.class}, statusCode);
                    if (statusCode >= 300 && statusCode <= 399) {
                        setHeader("Location", blockUrl);
                    }
                    setIntHeader(CONTENT_LENGTH_HEADER_KEY, script.getBytes().length);
                }
                sendContent(script, true);
            }
        }
    }

    // 把攻击类型等信息带入到返回响应内容中
    public void sendError(AttackInfo attackInfo, RaspConfig raspConfig) throws Exception {
        Context context = attackInfo.getContext();
        if (context != null) {
            Object response = context.getResponse();
            if (response != null) {
                int statusCode = raspConfig.getBlockStatusCode();
                String blockUrl = raspConfig.getRedirectUrl();
                boolean isCommitted = (Boolean) Reflection.invokeMethod(response, "isCommitted", new Class[]{});
                String contentType = context.getResponseContentType();
                String script;
                if (contentType != null && contentType.contains(CONTENT_TYPE_JSON_VALUE)) {
                    script = raspConfig.getJsonBlockContent();
                } else if (contentType != null && (contentType.contains(CONTENT_TYPE_XML_VALUE) || contentType.contains(CONTENT_TYPE_TEXT_XML))) {
                    script = raspConfig.getXmlBlockContent();
                } else {
                    script = raspConfig.getHtmlBlockContent();
                }
                // 插入当前攻击类型和时间戳
                script = script.replace("%attack_name%", attackInfo.getAttackType());
                script = script.replace("%attack_time%", String.valueOf(attackInfo.getAttackTime()));
                if (!isCommitted) {
                    resetBuffer();
                    Reflection.invokeMethod(response, "setStatus", new Class[]{int.class}, statusCode);
                    if (statusCode >= 300 && statusCode <= 399) {
                        setHeader("Location", blockUrl);
                    }
                    setIntHeader(CONTENT_LENGTH_HEADER_KEY, script.getBytes().length);
                }
                sendContent(script, true);
            }
        }
    }

    /**
     * 发送自定义错误处理脚本
     */
    public void sendContent(String content, boolean close) throws Exception {
        Object printer = null;

        printer = Reflection.invokeMethod(response, "getWriter", new Class[]{});
        if (printer == null) {
            printer = Reflection.invokeMethod(response, "getOutputStream", new Class[]{});
        }
        Reflection.invokeMethod(printer, "print", new Class[]{String.class}, content);
        Reflection.invokeMethod(printer, "flush", new Class[]{});
        if (close) {
            Reflection.invokeMethod(printer, "close", new Class[]{});
        }
    }

}