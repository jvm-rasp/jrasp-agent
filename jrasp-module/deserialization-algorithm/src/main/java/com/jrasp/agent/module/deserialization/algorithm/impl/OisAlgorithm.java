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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author jrasp
 */
public class OisAlgorithm implements Algorithm {

    private final RaspLog logger;

    private final String metaInfo;

    private final RaspConfig raspConfig;

    private Integer oisBlackListAction = 0;

    // jdk反序列化类白名单
    private Set<String> oisWhiteClassSet = new HashSet<String>();

    // jdk反序列化类黑名单
    private Set<String> oisBlackClassSet = new HashSet<String>(Arrays.asList(
            // 增加 fastjson、fastjson2 反序列化黑名单, 由 @是小易呀 提供
            "com.alibaba.fastjson.JSONArray",
            "com.alibaba.fastjson2.JSONArray",
            "org.codehaus.groovy.runtime.ConvertedClosure",
            "org.codehaus.groovy.runtime.ConversionHandler",
            "org.codehaus.groovy.runtime.MethodClosure",
            "org.springframework.transaction.support.AbstractPlatformTransactionManager",
            "java.rmi.server.UnicastRemoteObject",
            "java.rmi.server.RemoteObjectInvocationHandler",
            "com.bea.core.repackaged.springframework.transaction.support.AbstractPlatformTransactionManager",
            "java.rmi.server.RemoteObject",
            "com.tangosol.coherence.rest.util.extractor.MvelExtractor",
            "java.lang.Runtime",
            "oracle.eclipselink.coherence.integrated.internal.cache.LockVersionExtractor",
            "org.eclipse.persistence.internal.descriptors.MethodAttributeAccessor",
            "org.eclipse.persistence.internal.descriptors.InstanceVariableAttributeAccessor",
            "org.apache.commons.fileupload.disk.DiskFileItem",
            "oracle.jdbc.pool.OraclePooledConnection",
            "com.tangosol.util.extractor.ReflectionExtractor",
            "com.tangosol.internal.util.SimpleBinaryEntry",
            "com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$Storage$BinaryEntry",
            "com.sun.rowset.JdbcRowSetImpl",
            "org.eclipse.persistence.internal.indirection.ProxyIndirectionHandler",
            "bsh.XThis",
            "bsh.Interpreter",
            "com.mchange.v2.c3p0.PoolBackedDataSource",
            "com.mchange.v2.c3p0.impl.PoolBackedDataSourceBase",
            "org.apache.commons.beanutils.BeanComparator",
            "java.util.PriorityQueue",
            "java.lang.reflect.Proxy",
            "clojure.lang.PersistentArrayMap",
            "org.apache.commons.io.output.DeferredFileOutputStream",
            "org.apache.commons.io.output.ThresholdingOutputStream",
            "org.apache.wicket.util.upload.DiskFileItem",
            "org.apache.wicket.util.io.DeferredFileOutputStream",
            "org.apache.wicket.util.io.ThresholdingOutputStream",
            "com.sun.org.apache.bcel.internal.util.ClassLoader",
            "com.sun.syndication.feed.impl.ObjectBean",
            "org.springframework.beans.factory.ObjectFactory",
            "org.springframework.aop.framework.AdvisedSupport",
            "org.springframework.aop.target.SingletonTargetSource",
            "com.vaadin.data.util.NestedMethodProperty",
            "com.vaadin.data.util.PropertysetItem",
            "javax.management.BadAttributeValueExpException",
            "org.apache.myfaces.context.servlet.FacesContextImpl",
            "org.apache.myfaces.context.servlet.FacesContextImplBase"
    ));

    // 反序列化包黑名单
    private Set<String> oisBlackPackageSet = new HashSet<String>(Arrays.asList(
            // 增加 fastjson、fastjson2 反序列化包黑名单, 由 @是小易呀 提供
            "com.fasterxml.jackson.databind",
            "com.alibaba.fastjson2",
            "com.alibaba.fastjson",
            "org.apache.commons.collections.functors",
            "org.apache.commons.collections4.functors",
            "com.sun.org.apache.xalan.internal.xsltc.trax",
            "org.apache.xalan.xsltc.trax",
            "javassist",
            "java.rmi.activation",
            "sun.rmi.server",
            "com.bea.core.repackaged.springframework.aop.aspectj",
            "com.bea.core.repackaged.springframework.beans.factory.support",
            "org.python.core",
            "com.bea.core.repackaged.aspectj.weaver.tools.cache",
            "com.bea.core.repackaged.aspectj.weaver.tools",
            "com.bea.core.repackaged.aspectj.weaver.reflect",
            "com.bea.core.repackaged.aspectj.weaver",
            "com.oracle.wls.shaded.org.apache.xalan.xsltc.trax",
            "oracle.eclipselink.coherence.integrated.internal.querying",
            "oracle.eclipselink.coherence.integrated.internal.cache",
            "javax.swing.plaf.synth",
            "javax.swing.plaf.metal",
            "com.tangosol.internal.util.invoke",
            "com.tangosol.internal.util.invoke.lambda",
            "com.tangosol.util.extractor",
            "com.tangosol.coherence.rest.util.extractor",
            "com.tangosol.coherence.rest.util",
            "com.tangosol.coherence.component.application.console",
            "org.mozilla.javascript",
            "org.apache.myfaces.el",
            "org.apache.myfaces.view.facelets.el"
    ));

    public OisAlgorithm(RaspLog logger, RaspConfig raspConfig, String metaInfo) {
        this.logger = logger;
        this.metaInfo = metaInfo;
        this.raspConfig = raspConfig;
    }

    public OisAlgorithm(RaspLog logger, RaspConfig raspConfig, Map<String, String> configMaps, String metaInfo) {
        this(logger, raspConfig, metaInfo);
        this.oisBlackListAction = ParamSupported.getParameter(configMaps, "ois_black_list_action", Integer.class, oisBlackListAction);
        this.oisWhiteClassSet = ParamSupported.getParameter(configMaps, "ois_white_class_list", Set.class, oisWhiteClassSet);
        this.oisBlackClassSet = ParamSupported.getParameter(configMaps, "ois_black_class_list", Set.class, oisBlackClassSet);
        this.oisBlackPackageSet = ParamSupported.getParameter(configMaps, "ois_black_package_list", Set.class, oisBlackPackageSet);
    }

    @Override
    public String getType() {
        return "ois-deserialization";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (oisBlackListAction > -1) {
            if (parameters != null && parameters.length >= 1) {
                String className = (String) parameters[0];
                if (oisWhiteClassSet.contains(className)) {
                    return;
                }
                // 类名称匹配
                if (oisBlackClassSet.contains(className)) {
                    doCheck(context, className, oisBlackListAction, "deserialization class hit black list, class: " + className, 90);
                    return;
                }
                // 包名称匹配
                String pkg = StringUtils.isContainsPackage(className, oisBlackPackageSet);
                if (pkg != null) {
                    doCheck(context, className, oisBlackListAction, "deserialization class hit black list, package: " + pkg, 50);
                    return;
                }
            }
        }
    }

    @Override
    public String getDescribe() {
        return "ois deserialization algorithm";
    }

    private void doCheck(Context context, String className, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(context, metaInfo, className, enableBlock, "反序列化攻击", getDescribe(), message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("ois deserialization attack block by JRASP."));
        }
    }
}
