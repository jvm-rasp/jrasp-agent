package com.jrasp.agent.module.memoryShell.algorithm;

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

import java.util.Map;

@MetaInfServices(Module.class)
@Information(id = "memory-shell-algorithm", author = "yhlong")
public class MemoryShellAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private String metaInfo;

    private volatile Integer memoryShellAction = 0;

    @Override
    public boolean update(Map<String, String> configMaps) {
        algorithmManager.register(this);
        this.memoryShellAction = ParamSupported.getParameter(configMaps, "memory_shell_action", Integer.class, memoryShellAction);
        return true;
    }

    @Override
    public String getType() {
        return "内存马注入攻击";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        boolean enableBlock = memoryShellAction == 1;
        String message = "发现疑似内存马注入";
        AttackInfo attackInfo = new AttackInfo(
                context,
                metaInfo,
                "",
                enableBlock,
                "内存马注入防护",
                "memory shell inject",
                message,
                100);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("memory shell inject block by JRASP."));
        }
    }

    @Override
    public String getDescribe() {
        return "内存马注入攻击检测算法";
    }

    @Override
    public void loadCompleted() {
        algorithmManager.register(this);
    }

    private boolean isWhiteList(Context context) {
        return context != null
                && StringUtils.isBlank(context.getMethod())
                && StringUtils.isBlank(context.getRequestURI())
                && StringUtils.isBlank(context.getRequestURL());
    }
}
