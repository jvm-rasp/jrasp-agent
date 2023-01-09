package com.jrasp.agent.module.http.algorithm;

import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import org.kohsuke.MetaInfServices;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 主要是防护扫描器、ip、url
 */
@MetaInfServices(Module.class)
@Information(id = "http-algorithm", author = "jrasp")
public class HttpAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    /**
     * hook开关，默认开启，可以在管理端统一配置
     */
    private volatile Boolean disable = false;

    /**
     * ip 白名单（true）、黑名单（false）
     */
    private Map<String, Boolean> ipBlackMap = new ConcurrentHashMap<String, Boolean>() {
        {
            //put("0:0:0:0:0:0:0:1", false);
        }
    };

    /**
     * URL 黑名单
     */
    private Set<String> urlBlackSet = new CopyOnWriteArraySet<String>();

    /**
     * 扫描器特征：url
     */
    private Map<String/*特征*/, String/*扫描器名称*/> scanUrl = new ConcurrentHashMap(32) {
        {
            // awvs
            put("acunetix-wvs-test-for-some-inexistent-file", "nessus");
            put("by_wvs", "nessus");
            put("acunetix_wvs_security_test", "nessus");
            put("acunetix", "nessus");
            put("acunetix_wvs", "nessus");
            put("acunetix_test", "nessus");
            // nessus
            put("nessus", "nessus");
            put("Nessus", "nessus");
            // appscan
            put("Appscan", "appscan");
            // Rsas
            put("nsfocus", "Rsas");
            // sqlmap
            put("sqlmap", "sqlmap");
        }
    };

    /**
     * 扫描器特征：headers
     */
    private Map<String, String> scanHeaders = new ConcurrentHashMap() {
        {
            put("sqlmap", "sqlmap");
            put("appscan", "appscan");
            put("nessus", "nessus");
        }
    };

    /**
     * 扫描器特征：body 暂不用
     */
    private Map<String, String> scanBody = new ConcurrentHashMap() {
        {
            put("sqlmap", "sqlmap");
            put("appscan", "appscan");
            put("nessus", "nessus");
        }
    };

    @Override
    public void loadCompleted() {
        algorithmManager.register(this);
    }

    @Override
    public boolean update(Map<String, String> configMaps) {
        // 是否禁用检测
        String disableCheckStr = configMaps.get("disable");
        this.disable = Boolean.valueOf(disableCheckStr);
        return true;
    }

    @Override
    public String getType() {
        return "http";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (disable) {
            return;
        }
        if (context != null) {
            // ip 禁用
            String remoteHost = context.getRemoteHost();
            if (remoteHost != null) {
                // todo 需要加强，支持正则表达式
                if (ipBlackMap.containsKey(remoteHost)) {
                    AttackInfo attackInfo = new AttackInfo(context, remoteHost, true, "black ip", getDescribe(), "black ip: " + remoteHost, 95);
                    logger.attack(attackInfo);
                    ProcessControlException.throwThrowsImmediately(new RuntimeException("hit black ip: " + remoteHost));
                }
            }

            // 扫描器url特征
            String requestURL = context.getRequestURL();
            for (String key : scanUrl.keySet()) {
                if (requestURL.contains(key)) {
                    AttackInfo attackInfo = new AttackInfo(context, key, true, "scan url", getDescribe(), "scan url: " + key, 50);
                    logger.attack(attackInfo);
                    ProcessControlException.throwThrowsImmediately(new RuntimeException("hit url scan feature: " + requestURL));
                }
            }

            // 扫描器header特征
            String headerStr = context.getHeaderString();
            if (headerStr != null) {
                for (String key : scanHeaders.keySet()) {
                    if (headerStr.contains(key)) {
                        AttackInfo attackInfo = new AttackInfo(context, key, true, "scan header", getDescribe(), "scan header: " + key, 50);
                        logger.attack(attackInfo);
                        ProcessControlException.throwThrowsImmediately(new RuntimeException("hit header scan feature: " + key));
                    }
                }
            }

            // 禁用 url
            String contextRequestURL = context.getRequestURL();
            for (String url : urlBlackSet) {
                if (contextRequestURL.contains(url)) {
                    AttackInfo attackInfo = new AttackInfo(context, contextRequestURL, true, "block url", getDescribe(), "block url: " + url, 50);
                    logger.attack(attackInfo);
                    ProcessControlException.throwThrowsImmediately(new RuntimeException("hit black url: " + contextRequestURL));
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "http check";
    }

}
