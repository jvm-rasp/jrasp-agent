package com.jrasp.agent.module.jndi.algorithm;

import com.jrasp.agent.api.*;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StackTrace;
import com.jrasp.agent.api.util.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "jndi-algorithm", author = "jrasp")
public class JndiAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private String metaInfo;

    private volatile Integer jndiAction = 0;

    private volatile String[] jndiProtocolList = new String[]{"ladp://", "rmi://"};

    private volatile Set<String> jndiDangerStackSet = new HashSet<String>(Arrays.asList(
            "java.beans.XMLDecoder.readObject",
            "com.caucho.hessian.io.HessianInput.readObject",
            "org.apache.dubbo.common.serialize.hessian2.Hessian2ObjectInput.readObject",
            "org.yaml.snakeyaml.Yaml.load",
            "org.apache.logging.log4j.core.net.JndiManager.lookup",
            "ysoserial.Pwner",
            "java.sql.DriverManager.getConnection",
            "java.io.ObjectInputStream.readObject",
            "com.alibaba.fastjson.JSON.parse",
            "com.fasterxml.jackson.databind.ObjectMapper.readValue",
            "com.thoughtworks.xstream.XStream.unmarshal"
    ));

    @Override
    public void loadCompleted() {
        algorithmManager.register(this);
    }

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.jndiAction = ParamSupported.getParameter(configMaps, "jndi_action", Integer.class, jndiAction);
        this.jndiProtocolList = ParamSupported.getParameter(configMaps, "jndi_protocol_list", String[].class, jndiProtocolList);
        this.jndiDangerStackSet = ParamSupported.getParameter(configMaps, "jndi_danger_stack_list", Set.class, jndiDangerStackSet);
        return true;
    }

    @Override
    public String getType() {
        return "jndi";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (jndiAction > -1) {
            if (parameters != null) {
                String name = (String) parameters[0];
                String protocol = containsProtocol(name);
                if (protocol != null) {
                    // 算法1: 栈特征检测
                    String[] stackTraceString = StackTrace.getStackTraceString(100, false);
                    for (String stack : stackTraceString) {
                        if (jndiDangerStackSet.contains(stack)) {
                            doAction(context, name, jndiAction, "jndi stacks contains danger stack: " + stack, 100);
                            return;
                        }
                    }
                    // 算法2: 记录日志
                    doAction(context, name, jndiAction, "jndi url contains danger protocol: " + protocol, 90);
                }
            }

        }
    }

    private String containsProtocol(String name) {
        if (name != null) {
            if (StringUtils.isNotBlank(name)) {
                for (String protocol : jndiProtocolList) {
                    if (name.contains(protocol)) {
                        return protocol;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getDescribe() {
        return "jndi check";
    }

    private void doAction(Context context, String lookupUrl, int action, String message, int level) throws ProcessControlException {
        boolean enableBlock = action == 1;
        AttackInfo attackInfo = new AttackInfo(
                context,
                metaInfo,
                lookupUrl,
                enableBlock,
                "jndi",
                getDescribe(),
                message, level);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("jndi block by JRASP."));
        }
    }
}
