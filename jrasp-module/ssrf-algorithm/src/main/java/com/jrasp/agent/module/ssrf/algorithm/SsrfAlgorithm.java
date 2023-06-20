package com.jrasp.agent.module.ssrf.algorithm;

import com.jrasp.agent.api.*;
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

import java.net.URL;
import java.util.*;

/**
 * @author jrasp
 * 算法来源于 openrasp
 */
@MetaInfServices(Module.class)
@Information(id = "ssrf-algorithm", author = "jrasp")
public class SsrfAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    private volatile Integer ssrfAction = 1;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private String metaInfo;

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.ssrfAction = ParamSupported.getParameter(configMaps, "ssrf_action", Integer.class, ssrfAction);
        this.ssrfBlackHostname = ParamSupported.getParameter(configMaps, "ssrf_black_hostname", Set.class, ssrfBlackHostname);
        algorithmManager.register(this);
        return true;
    }

    @Override
    public String getType() {
        return "ssrf";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        if (context != null && parameters != null && parameters.length > 0 && parameters[0] instanceof Map) {
            Map<String, Object> params = (Map) parameters[0];
            String hostName = (String) params.get("host");
            String ip = null;
            if (params.get("ip") instanceof String) {
                ip = (String) params.get("ip");
            } else if (params.get("ip") instanceof LinkedList) {
                List<String> ips = (LinkedList<String>) params.get("ip");
                if (ips.size() > 0) {
                     ip = ips.get(0);
                }
            }
            if (ip == null || hostName == null) {
                return;
            }
            String url = (String) params.get("url");
            Boolean isRedirect = (Boolean) params.get("isRedirect");
            Object redirectUrl = context.getObject("redirectUrl");

            if (redirectUrl instanceof String) {
                isRedirect = StringUtils.isNotBlank((CharSequence) redirectUrl);
                if (isRedirect) {
                    url = (String) redirectUrl;
                }
            }
            // TODO 算法1：当参数来自用户输入，且为内网IP，判定为SSRF攻击

            /*if (isFromUserInputURL(getRequestParams(context), new URL(url))) {
                doActionCtl(ssrfAction, context, hostName, "SSRF - Requesting suspicious address", "", 60);
                return;
            }*/

            // 算法2：检查常见探测域名
            String hostnameDnslog = isHostnameDnslog(hostName);
            if (hostnameDnslog != null) {
                doActionCtl(ssrfAction, context, hostName, "SSRF - Requesting known DNSLOG address: " + hostnameDnslog, "", 60);
                return;
            }

            // 算法3 - 检测 AWS/Aliyun/GoogleCloud 私有地址: 拦截IP访问、绑定域名访问两种方式
            if (awsMetaDataAddressSet.contains(hostName) || awsMetaDataAddressSet.contains(ip)) {
                doActionCtl(ssrfAction, context, hostName, "SSRF - Requesting AWS metadata address", "", 60);
                return;
            }

            // 算法4 - hostname 混淆
            // 检查混淆:
            // http://2130706433  http://0x7f001
            // http://0x7f.0x0.0x0.0x1  http://0x7f.0.0.0
            if (hostName.length() != 0 && !isNaN(hostName)) {
                doActionCtl(ssrfAction, context, hostName, "SSRF - Requesting hexadecimal IP address", "", 60);
                return;
            }

            // 算法5 - 特殊协议检查
            if (url != null) {
                String[] protoAndUrl = url.split(":");
                if (protoAndUrl != null && protoAndUrl.length >= 2) {
                    String proto = protoAndUrl[0];
                    if (proto != null && protocolsSet.contains(proto.toLowerCase().trim())) {
                        doActionCtl(ssrfAction, context, hostName, "SSRF - Using dangerous protocol: " + proto, "", 60);
                        return;
                    }
                }
            }
        }
    }

    // 处理 Tomcat 启动时注入防护 Agent 产生的误报情况
    private boolean isWhiteList(Context context) {
        return context != null
                && StringUtils.isBlank(context.getMethod())
                && StringUtils.isBlank(context.getRequestURI())
                && StringUtils.isBlank(context.getRequestURL());
    }

    private void doActionCtl(int action, Context context, String payload, String algorithm, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context,metaInfo, payload, enableBlock, "SSRF请求伪造", algorithm, message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("ssrf block by EpointRASP."));
        }
    }

    @Override
    public String getDescribe() {
        return "ssrf check algorithm";
    }

    private Set<String> awsMetaDataAddressSet = new HashSet<String>(Arrays.asList(
            // Azure DNS IP 位址是 168.63.129.16
            "169.254.169.254", "100.100.100.200", "168.63.129.16", "metadata.google.internal", "metadata.tencentyun.com"));

    private Set<String> protocolsSet = new HashSet<String>(Arrays.asList(
            "file", "gopher", "jar", "netdoc", "mailto"));

    private Set<String> ssrfBlackHostname = new HashSet<String>(Arrays.asList(
            ".vuleye.pw", ".ceye.io", ".exeye.io", ".vcap.me", ".xip.name", ".xip.io", ".sslip.io", ".nip.io",
            ".burpcollaborator.net", ".tu4.org", ".2xss.cc", ".bxss.me", ".godns.vip", ".dnslog.cn", ".0kee.360.cn", ".r87.me", ".ngrok.io",
            // yumusb/DNSLog-Platform-Golang
            ".xn--9tr.com",
            // requestbin 新地址
            ".pipedream.net",
            // 端口转发工具
            ".vxtrans.com", ".vxtrans.link",
            // 免费DDNS厂商
            ".hopto.org", ".zapto.org", ".sytes.net", ".ddns.net",
            // 其它国外域名
            ".oast.pro", ".oast.live", ".oast.site", ".oast.online", ".oast.fun", ".oast.me", ".1433.eu.org",
            ".dns.bypass.eu.org", ".dnslog.pw", ".eyes.sh", ".dnslog.link", ".dnslog.io", ".imgcdnns.com",
            ".ns.dns3.cf", ".dnslog.cool", ".yunzhanghu.co", ".dnslog.run", ".s0x.cn", ".awvsscan119.autoverify.cn",
            ".360-cert.com", ".microcoft.cyou", ".mauu.me", ".0x557.wang", ".cybertunnel.run"
    ));

    private String[] hostNameContains = new String[]{"requestb.in", "transfer.sh"};

    private String isHostnameDnslog(String hostName) {
        if (hostName != null) {
            hostName = hostName.toLowerCase();
            for (String item : ssrfBlackHostname) {
                if (hostName.endsWith(item)) {
                    return item;
                }
            }
            for (String item : hostNameContains) {
                if (hostName.contains(item)) {
                    return item;
                }
            }
        }
        return null;
    }

    private boolean isNumber(String number) {
        try {
            Long.parseLong(number);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNaN(String val) {
        try {
            Long.parseLong(val, 10);
            return false;
        } catch (NumberFormatException numberFormatException) {
            try {
                if (val.toLowerCase().startsWith("0x")) {
                    val = val.substring(2);
                }
                Long.parseLong(val, 16);
                return false;
            } catch (NumberFormatException numberFormatException1) {
                try {
                    if (val.toLowerCase().startsWith("0b")) {
                        val = val.substring(2);
                    }
                    Long.parseLong(val, 2);
                    return false;
                } catch (NumberFormatException numberFormatException2) {
                    try {
                        if (val.toLowerCase().startsWith("0o")) {
                            val = val.substring(2);
                        }
                        Long.parseLong(val, 8);
                        return false;
                    } catch (NumberFormatException numberFormatException3) {
                        return true;
                    }
                }
            }
        }
    }

    private static boolean isFromUserInputURL(String[] parameters, URL target) {
        if (parameters == null || parameters.length == 0 || target == null) {
            return false;
        }
        String urlPath = target.getPath();
        for (String param : parameters) {
            if (param.contains(urlPath)) {
                return true;
            }
        }
        return false;
    }

    private String[] getRequestParams(Context context) {
        List<String> params = new ArrayList<String>();
        if (context.getDecryptParameters() != null) {
            for (Map.Entry<String, String[]> entry : context.getDecryptParameters().entrySet()) {
                params.addAll(Arrays.asList(entry.getValue()));
            }
        }
        if (context.getHeader() != null) {
            for (Map.Entry<String, String> entry : context.getHeader().entrySet()) {
                params.add(entry.getValue());
            }
        }

        return params.toArray(new String[0]);
    }
}
