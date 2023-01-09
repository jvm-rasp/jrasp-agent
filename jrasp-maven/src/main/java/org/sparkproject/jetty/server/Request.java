package org.sparkproject.jetty.server;

import java.util.Enumeration;
import java.util.Map;

public class Request {

    public String getLocalAddr() {
        return "";
    }

    public String getMethod() {
        return "";
    }

    public int getContentLength() {
        return 1;
    }

    public String getContentType() {
        return "";
    }

    public String getQueryString() {
        return "";
    }

    public String getProtocol() {
        return "";
    }

    public String getRemoteHost() {
        return "";
    }

    public StringBuffer getRequestURL() {
        return null;
    }

    public String getRequestURI() {
        return "";
    }

    public Map<String, String[]> getParameterMap() {
        return null;
    }

    public Enumeration<String> getHeaderNames() {
        return null;
    }

    public String getHeader(String key) {
        return "";
    }
}
