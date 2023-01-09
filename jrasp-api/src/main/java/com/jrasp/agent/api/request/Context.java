package com.jrasp.agent.api.request;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jrasp.agent.api.util.StringUtils.array2String;

/**
 * 请求上下文
 * 使用toString 生成 json
 *
 * @author jrasp
 */
public class Context {

    // 字段1
    private String method;

    // 字段2
    private String protocol;

    // 字段3
    private String localAddr;

    // 字段4
    private String remoteHost;

    // 字段5
    private String requestURL;

    // 字段6
    private String requestURI;

    // 字段7
    private String contentType;

    // 字段8
    private int contentLength;

    // 字段9
    private String characterEncoding;

    // 字段10
    private Map<String, String[]> parameters;

    // 字段11
    private Map<String, String> header;

    // 字段12
    private String queryString;

    // 字段13 上下文标记
    private Set<String> marks = new HashSet<String>();

    // 字段14 上下对象
    private Map<String, Object> attach = new HashMap<String, Object>();

    private final int maxBodySize = 4096;

    // 原始 http request 对象
    private Object request;

    private Object inputStream = null;

    private Object charReader = null;

    private ByteArrayOutputStream bodyOutputStream = null;

    private CharArrayWriter bodyWriter = null;

    public void setRequest(Object request) {
        this.request = request;
    }

    public Object getRequest() {
        return this.request;
    }

    public byte[] getBody() {
        return bodyOutputStream != null ? bodyOutputStream.toByteArray() : null;
    }

    public String getStringBody() {
        if (bodyOutputStream != null) {
            return bodyOutputStream.toString();
        } else if (bodyWriter != null) {
            return bodyWriter.toString();
        }
        return null;
    }

    // h1:v1;h2:v2;h3:v3;h4:v4
    // TODO 转义
    public String getHeaderString() {
        if (header != null && header.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : header.entrySet()) {
                String s = entry.getValue().replaceAll("\"", "\\\\\"");
                sb.append(entry.getKey()).append(":").append(s).append(";");
            }
            //  去掉最后一个;
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    // TODO 转义
    public String getParametersString() {
        if (parameters != null && parameters.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                String key = entry.getKey();
                for (String v : entry.getValue()) {
                    String s = v.replaceAll("\"", "\\\\\"");
                    sb.append(key).append("=").append(s).append("&");
                }
            }
            //  去掉最后一个&
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    public String getMarks() {
        if (marks.size() == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String mark : marks) {
            stringBuilder.append(mark);
            stringBuilder.append(",");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public ByteArrayOutputStream getBodyStream() {
        return bodyOutputStream;
    }

    public Object getInputStream() {
        return inputStream;
    }

    public void setInputStream(Object inputStream) {
        this.inputStream = inputStream;
    }

    public Object getCharReader() {
        return charReader;
    }

    public void setCharReader(Object charReader) {
        this.charReader = charReader;
    }

    public void appendByteBody(int b) {
        if (bodyOutputStream == null) {
            bodyOutputStream = new ByteArrayOutputStream();
        }

        if (bodyOutputStream.size() < maxBodySize) {
            bodyOutputStream.write(b);
        }
    }

    public void appendBody(byte[] bytes, int offset, int len) {
        if (bodyOutputStream == null) {
            bodyOutputStream = new ByteArrayOutputStream();
        }

        len = Math.min(len, maxBodySize - bodyOutputStream.size());
        if (len > 0) {
            bodyOutputStream.write(bytes, offset, len);
        }
    }

    public void appendBody(char[] cbuf, int offset, int len) {
        if (bodyWriter == null) {
            bodyWriter = new CharArrayWriter();
        }

        len = Math.min(len, maxBodySize / 2 - bodyWriter.size());
        if (len > 0) {
            bodyWriter.write(cbuf, offset, len);
        }
    }

    public void appendCharBody(int b) {
        if (bodyWriter == null) {
            bodyWriter = new CharArrayWriter();
        }

        if (bodyWriter.size() < (maxBodySize / 2)) {
            bodyWriter.write(b);
        }
    }

    public ByteArrayOutputStream getBodyOutputStream() {
        return bodyOutputStream;
    }

    public void setBodyOutputStream(ByteArrayOutputStream bodyOutputStream) {
        this.bodyOutputStream = bodyOutputStream;
    }

    public CharArrayWriter getBodyWriter() {
        return bodyWriter;
    }

    public void setBodyWriter(CharArrayWriter bodyWriter) {
        this.bodyWriter = bodyWriter;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public void setLocalAddr(String localAddr) {
        this.localAddr = localAddr;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    public void setParameterMap(Map<String, String[]> parameterMap) {
        this.parameters = parameterMap;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public void addMark(String mark) {
        marks.add(mark);
    }

    public void remove(String mark) {
        marks.remove(mark);
    }

    public boolean hasMark(String mark) {
        return marks.contains(mark);
    }

    public void addObject(String name, Object value) {
        attach.put(name, value);
    }

    public Object getObject(String name) {
        return attach.get(name);
    }

    public String toJSON() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"method\":\"")
                .append(method).append('\"');
        sb.append(",\"protocol\":\"")
                .append(protocol).append('\"');
        sb.append(",\"localAddr\":\"")
                .append(localAddr).append('\"');
        sb.append(",\"remoteHost\":\"")
                .append(remoteHost).append('\"');
        sb.append(",\"requestURL\":\"")
                .append(requestURL).append('\"');
        sb.append(",\"requestURI\":\"")
                .append(requestURI).append('\"');
        sb.append(",\"contentType\":\"")
                .append(contentType).append('\"');
        sb.append(",\"contentLength\":")
                .append(contentLength);
        sb.append(",\"characterEncoding\":\"")
                .append(characterEncoding).append('\"');
        sb.append(",\"parameters\":\"")
                .append(getParametersString()).append('\"');
        sb.append(",\"header\":\"")
                .append(getHeaderString()).append('\"');
        sb.append(",\"queryString\":\"")
                .append(getQueryString()).append('\"');
        sb.append(",\"marks\":\"")
                .append(getMarks()).append('\"');
        // 直接输出byte[]
        sb.append(",\"body\":\"")
                .append(array2String(getBody())).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
