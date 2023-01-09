package com.jrasp.agent.module.ssrf.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jrasp
 * 算法来源于 openrasp
 */
@MetaInfServices(Module.class)
@Information(id = "ssrf-algorithm", author = "jrasp")
public class SsrfAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    private volatile Integer action = 0;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    @Override
    public boolean update(Map<String, String> configMaps) {
        return false;
    }

    @Override
    public String getType() {
        return "ssrf";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (action > -1 && context != null && parameters != null) {
            Map<String, Object> params = (Map) parameters[0];
            String hostName = (String) params.get("host");
            String ip = (String) params.get("ip");
            String url = (String) params.get("url");
            Boolean isRedirect = (Boolean) params.get("isRedirect");
            // 算法1：当参数来自用户输入，且为内网IP，判定为SSRF攻击
            if(isRedirect){

            }

            // 算法2：检查常见探测域名
            String hostnameDnslog = isHostnameDnslog(hostName);
            if (hostnameDnslog != null) {
                doActionCtl(action, context, hostName, "SSRF - Requesting known DNSLOG address: " + hostnameDnslog, "", 60);
                return;
            }

            // 算法3 - 检测 AWS/Aliyun/GoogleCloud 私有地址: 拦截IP访问、绑定域名访问两种方式
            if (awsMetaDataAddressSet.contains(hostName) || awsMetaDataAddressSet.contains(ip)) {
                doActionCtl(action, context, hostName, "SSRF - Requesting AWS metadata address", "", 60);
                return;
            }

            // 算法4 - hostname 混淆
            // 检查混淆:
            // http://2130706433  http://0x7f001
            // http://0x7f.0x0.0x0.0x1  http://0x7f.0.0.0
            if (hostName.contains("0x") || isNumber(hostName)) {
                doActionCtl(action, context, hostName, "SSRF - Requesting hexadecimal IP address", "", 60);
                return;
            }

            // 算法5 - 特殊协议检查
            if (url != null) {
                String[] protoAndUrl = url.split(":");
                if (protoAndUrl != null && protoAndUrl.length >= 2) {
                    String proto = protoAndUrl[0];
                    if (proto != null && protocolsSet.contains(proto.toLowerCase().trim())) {
                        doActionCtl(action, context, hostName, "SSRF - Using dangerous protocol: " + proto, "", 60);
                        return;
                    }
                }
            }
        }
    }

    private void doActionCtl(int action, Context context, String payload, String algorithm, String message, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, payload, enableBlock, getType(), algorithm, message, level);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessControlException.throwThrowsImmediately(new RuntimeException("ssrf block by rasp."));
            }
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
            "file", "gopher", "jar", "netdoc"));

    private String[] hostNameEndsWith = new String[]{
            ".vuleye.pw",
            ".ceye.io",
            ".exeye.io",
            ".vcap.me",
            ".xip.name",
            ".xip.io",
            ".sslip.io",
            ".nip.io",
            ".oastify.com",
            ".eyes.sh",
            ".burpcollaborator.net",
            ".tu4.org",
            ".2xss.cc",
            ".bxss.me",
            ".godns.vip",
            ".dnslog.cn",
            ".0kee.360.cn",
            ".r87.me",
            ".ngrok.io",
            ".xn--9tr.com",
            ".pipedream.net",
            ".vxtrans.com",
            ".vxtrans.link",
            ".hopto.org",
            ".zapto.org",
            ".sytes.net",
            ".ddns.net"
    };

    private String[] hostNameContains = new String[]{"requestb.in", "transfer.sh"};

    private String isHostnameDnslog(String hostName) {
        if (hostName != null) {
            hostName = hostName.toLowerCase();
            for (String item : hostNameEndsWith) {
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

}
