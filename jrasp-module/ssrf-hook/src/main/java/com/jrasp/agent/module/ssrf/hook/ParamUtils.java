package com.jrasp.agent.module.ssrf.hook;

import com.jrasp.agent.api.listener.Advice;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ParamUtils {
    public static HashMap<String, Object> getSsrfParam(String url, String host, String port, String client) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("url", url);
        params.put("hostname", host);
        params.put("client", client);
        params.put("port", port);
        LinkedList<String> ip = getIpList(host);
        Collections.sort(ip);
        params.put("ip", ip);
        return params;
    }

    private static LinkedList<String> getIpList(String hostname) {
        LinkedList<String> ip = new LinkedList<String>();
        if (hostname.isEmpty())
            return ip;
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            for (InetAddress address : addresses) {
                if (address instanceof java.net.Inet4Address)
                    ip.add(address.getHostAddress());
            }
        } catch (Throwable throwable) {
        }
        return ip;
    }

    public static Map<String, Object> getRedirectParams(Advice advice, Map<String, Object> params) {
        Map<String, Object> newParams = new HashMap<String, Object>();
        newParams.put("origin_hostname", params.get("hostname"));
        newParams.put("origin_ip", params.get("ip"));
        newParams.put("origin_url", params.get("url"));
        newParams.put("url", params.get("url2"));
        newParams.put("hostname", params.get("hostname2"));
        newParams.put("ip", params.get("ip2"));
        newParams.put("port", params.get("port2"));
        newParams.put("client", params.get("client"));
        return newParams;
    }
}
