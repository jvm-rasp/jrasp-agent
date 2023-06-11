package com.jrasp.agent.module.deserialization.algorithm.impl;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;

import java.util.*;

/**
 * @author jrasp
 */
public class XmlAlgorithm implements Algorithm {

    private final RaspLog logger;

    private final String metaInfo;

    private final RaspConfig raspConfig;

    private Integer xmlBlackListAction = 0;

    // xml反序列化类白名单
    private Set<String> xmlWhiteClassSet = new HashSet<String>();

    //  xml反序列化类黑名单
    private Set<String> xmlBlackClassSet = new HashSet<String>(Arrays.asList(
            "java.io.PrintWriter", "java.io.FileInputStream", "java.io.FileOutputStream", "java.util.PriorityQueue",
            "javax.sql.rowset.BaseRowSet", "javax.activation.DataSource", "java.nio.channels.Channel", "java.io.InputStream",
            "java.lang.ProcessBuilder", "java.lang.Runtime", "javafx.collections.ObservableList", "java.beans.EventHandler", "sun.swing.SwingLazyValue", "java.io.File"
    ));

    // xml反序列化包黑名单
    private Set<String> xmlBlackPackageSet = new HashSet<String>(Arrays.asList(
            "sun.reflect", "sun.tracing", "com.sun.corba", "javax.crypto", "jdk.nashorn.internal",
            "sun.awt.datatransfer", "com.sun.tools", "javax.imageio", "com.sun.rowset"
    ));

    //  xml反序列化关键字黑名单
    private List<String> xmlBlackKeyList = Arrays.asList(
            ".jndi.", ".rmi.", ".bcel.", ".xsltc.trax.TemplatesImpl", ".ws.client.sei.",
            "$URLData", "$LazyIterator", "$GetterSetterReflection", "$PrivilegedGetter", "$ProxyLazyValue", "$ServiceNameIterator"
    );

    public XmlAlgorithm(RaspLog logger, RaspConfig raspConfig, String metaInfo) {
        this.logger = logger;
        this.metaInfo = metaInfo;
        this.raspConfig = raspConfig;
    }

    public XmlAlgorithm(RaspLog logger, RaspConfig raspConfig, Map<String, String> configMaps, String metaInfo) {
        this(logger, raspConfig, metaInfo);
        this.xmlBlackListAction = ParamSupported.getParameter(configMaps, "xml_black_list_action", Integer.class, xmlBlackListAction);
        this.xmlWhiteClassSet = ParamSupported.getParameter(configMaps, "xml_white_list_action", Set.class, xmlWhiteClassSet);
        this.xmlBlackClassSet = ParamSupported.getParameter(configMaps, "xml_black_class_list", Set.class, xmlBlackClassSet);
        this.xmlBlackPackageSet = ParamSupported.getParameter(configMaps, "xml_black_package_list", Set.class, xmlBlackPackageSet);
        this.xmlBlackKeyList = ParamSupported.getParameter(configMaps, "xml_black_key_list", List.class, xmlBlackKeyList);
    }

    @Override
    public String getType() {
        return "xml-deserialization";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (xmlBlackListAction > -1) {
            if (parameters != null && parameters.length >= 1) {
                String className = (String) parameters[0];
                // 白名单
                if (xmlWhiteClassSet.contains(className)) {
                    return;
                }

                // 类名称匹配
                if (xmlBlackClassSet.contains(className)) {
                    doAction(context, className, xmlBlackListAction, "deserialization class hit black list, class: " + className, 90);
                    return;
                }

                // 包名称匹配
                String pkg = StringUtils.isContainsPackage(className, xmlBlackPackageSet);
                if (pkg != null) {
                    doAction(context, className, xmlBlackListAction, "deserialization class hit black list, package: " + pkg, 80);
                    return;
                }

                // 关键字黑名单
                for (String key : xmlBlackKeyList) {
                    if (className.contains(key)) {
                        doAction(context, className, xmlBlackListAction, "deserialization class hit black list, key: " + key, 50);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "xml deserialization algorithm";
    }

    private void doAction(Context context, String className, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context, metaInfo, className, enableBlock, getType(), getDescribe(), message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("xml deserialization attack block by JRASP."));
        }
    }

}
