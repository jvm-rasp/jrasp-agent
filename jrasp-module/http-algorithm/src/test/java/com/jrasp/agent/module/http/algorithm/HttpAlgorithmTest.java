package com.jrasp.agent.module.http.algorithm;

import com.jrasp.agent.api.request.Context;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class HttpAlgorithmTest {

    @Test
    public void checkHeaderScanTest() {
        Map<String, String> header = new HashMap<String, String>();
        header.put("Date", "Thu, 20 Jul 2023 15:56:46 GMT");
        header.put("Vary", "Accept-Encoding");
        header.put("Connection", "keep-alive");
        header.put("Access-Control-Allow-Origin", "https://blog.csdn.net");
        header.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        header.put("Access-Control-Allow-Headers", "        content-type,user_name,uber-trace-id,Cookie,Origin,Accept,X-Requested-With,Connection,User-Agent,Referer,Sec-Fetch-Dest,Sec-Fetch-Site,Host,Accept-Encoding,DNT,Sec-Fetch-Mode,X-Yundun-Origin,X-Forwarded-For,Accept-Language,Content-Length,X-Real-IP");
        header.put("Access-Control-Max-Age", "86400");
        header.put("Strict-Transport-Security", "max-age=0; preload");
        header.put("X-Cache", "BYPASS");
        header.put("X-Request-Id", "fab772b14742df77af656793031292c8");
        header.put("Server", "WAF");
        header.put("Content-Encoding", "gzip");

        Context context = new Context();
        context.setHeader(header);

        // 正常
        String headerString = context.getHeaderString();
        HttpAlgorithm httpAlgorithm = new HttpAlgorithm();
        String key1 = httpAlgorithm.checkHeaderScan(headerString);
        assert key1 == null;

        // 检测到扫描器特征
        header.put("User Agent", "sqlmap/1.6.12.5#dev (https://sqlmap.org)");
        String key2 = httpAlgorithm.checkHeaderScan(context.getHeaderString());
        assert key2.equals("sqlmap");

        // 修复单一的contains误报
        // content-type 中含有是扫描器字符XRay，但是不属于content-type分割后的token不含XRay
        header.put("User Agent", "");
        header.put("content-type", "multipart/form-dat;boundary=5JuZcac23cs01XRayeY36Y");
        String key3 = httpAlgorithm.checkHeaderScan(context.getHeaderString());
        assert key3 == null;
    }
}
