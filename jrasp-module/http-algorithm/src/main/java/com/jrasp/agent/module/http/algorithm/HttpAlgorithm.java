package com.jrasp.agent.module.http.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 主要是防护扫描器、ip、url
 */
@MetaInfServices(Module.class)
@Information(id = "http-algorithm", author = "jrasp")
public class HttpAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private String metaInfo;

    private volatile Integer ipBlackListAction = 0;

    private volatile Integer urlBlackListAction = 0;

    private volatile Integer scanListAction = 0;

    /**
     * ip 黑名单
     */
    private Set<String> ipBlackSet = new HashSet<String>();

    /**
     * URL 黑名单
     */
    private Set<String> urlBlackSet = new HashSet<String>();

    /**
     * 扫描器特征：url
     */
    private Set<String/*特征*/> scanUrlSet = new HashSet<String>(Arrays.asList(
            // awvs
            "acunetix-wvs-test-for-some-inexistent-file",
            "by_wvs",
            "acunetix_wvs_security_test",
            "acunetix",
            "acunetix_wvs",
            "acunetix_test",
            // nessus
            "nessus",
            "Nessus",
            // appscan
            "Appscan",
            // Rsas
            "nsfocus",
            // sqlmap
            "sqlmap"
    ));

    /**
     * 扫描器特征：headers
     * 扫描器特征：body 暂不用
     */
    private Set<String> scanHeadersOrBody = new HashSet<String>(Arrays.asList(
            "sqlmap", "appscan", "nessus", "acunetix", "netsparker", "webinspect", "Rsas", "nsfocus", "WebReaver",
            "zgrab", "goby", "xray"
    ));

    @Override
    public void loadCompleted() {
        algorithmManager.register(this);
    }

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.ipBlackListAction = ParamSupported.getParameter(configMaps, "ip_black_list_action", Integer.class, ipBlackListAction);
        this.urlBlackListAction = ParamSupported.getParameter(configMaps, "url_black_list_action", Integer.class, urlBlackListAction);
        this.scanListAction = ParamSupported.getParameter(configMaps, "scan_list_action", Integer.class, scanListAction);
        this.ipBlackSet = ParamSupported.getParameter(configMaps, "ip_black_list", Set.class, ipBlackSet);
        this.urlBlackSet = ParamSupported.getParameter(configMaps, "url_black_set", Set.class, urlBlackSet);
        this.scanUrlSet = ParamSupported.getParameter(configMaps, "scan_url_set", Set.class, scanUrlSet);
        this.scanHeadersOrBody = ParamSupported.getParameter(configMaps, "scan_header_set", Set.class, scanHeadersOrBody);
        return true;
    }

    @Override
    public String getType() {
        return "http";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (context != null) {
            // ip 禁用
            if (ipBlackListAction > -1) {
                String remoteHost = context.getRemoteHost();
                if (remoteHost != null) {
                    // todo 需要加强，支持正则表达式
                    if (ipBlackSet.contains(remoteHost)) {
                        boolean canBlock = ipBlackListAction == 1;
                        AttackInfo attackInfo = new AttackInfo(context, metaInfo, remoteHost, canBlock, "black ip", getDescribe(), "black ip: " + remoteHost, 95);
                        logger.attack(attackInfo);
                        if (canBlock) {
                            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("hit black ip: " + remoteHost));
                        }
                    }
                }
            }

            // 扫描器url特征
            if (scanListAction > -1) {
                String requestURL = context.getRequestURL();
                for (String key : scanUrlSet) {
                    if (requestURL.contains(key)) {
                        boolean canBlock = scanListAction == 1;
                        AttackInfo attackInfo = new AttackInfo(context, metaInfo, key, canBlock, "scan url", getDescribe(), "scan url: " + key, 50);
                        logger.attack(attackInfo);
                        if (canBlock) {
                            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("hit url scan feature: " + requestURL));
                        }
                    }
                }

                // 扫描器header特征
                String headerStr = context.getHeaderString();
                if (headerStr != null) {
                    for (String key : scanHeadersOrBody) {
                        // TODO headerStr.toLowerCase() 只调用一次
                        if (headerStr.toLowerCase().contains(key.toLowerCase())) {
                            boolean canBlock = scanListAction == 1;
                            AttackInfo attackInfo = new AttackInfo(context, metaInfo, key, canBlock, "扫描器扫描", getDescribe(), "scan header: " + key, 50);
                            logger.attack(attackInfo);
                            if (canBlock) {
                                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("hit header scan feature: " + key));
                            }
                        }
                    }
                }
            }

            // 禁用 url
            if (urlBlackListAction > -1) {
                String contextRequestURL = context.getRequestURL();
                for (String url : urlBlackSet) {
                    if (contextRequestURL.contains(url)) {
                        boolean canBlock = urlBlackListAction == 1;
                        AttackInfo attackInfo = new AttackInfo(context, metaInfo, contextRequestURL, canBlock, "block url", getDescribe(), "block url: " + url, 50);
                        logger.attack(attackInfo);
                        if (canBlock) {
                            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("hit black url: " + contextRequestURL));
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "http check";
    }

}
