package com.jrasp.agent.api.request;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ContextTest {
    @Test
    public void headerTest() {
        Context context = new Context();
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("content-length", "840");
        headerMap.put("referer", "http://192.168.168.40:8087/raspdemo/frame/pages/login/login;accept-language:zh-CN,zh;q=0.9");
        headerMap.put("cookie", "_theme_=idea; _font_size_ratio_=1.0; _idea_skin_=cyanine; sid=9DE6E35CF91D45CA8B3E2E8613E8C685; _loginUsername_=admin; _loginType_=0");
        headerMap.put("origin", "http://192.168.168.40:8087");
        headerMap.put("accept", "application/json, text/javascript, */*; q=0.01");
        headerMap.put("host", "192.168.168.40:8087");
        headerMap.put("x-requested-with", "XMLHttpRequest");
        headerMap.put("connection", "keep-alive");
        headerMap.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8;user-token:userSign=,reqTime=1679557723548,deviceId=86ae2ea2f4b55c369d8c82e5215d292d,title=%E7%99%BB%E5%BD%95");
        headerMap.put("accept-encoding", "gzip, deflate");
        headerMap.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
        context.setHeader(headerMap);

        String headerString = context.getHeaderString();
        String[] kvs = headerString.split(System.getProperty("line.separator"));
        for (String kv : kvs) {
            String[] kvArray = kv.split(":", 2);
            assert kvArray != null && kvArray.length == 2;
            String key = kvArray[0];
            String value = kvArray[1];
            assert headerMap.get(key).equals(value);
        }
        assert kvs != null && kvs.length == headerMap.size();
    }
}
