package com.jrasp.agent.module.expression.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.module.expression.algorithm.impl.OgnlAlgorithm;
import com.jrasp.agent.module.expression.algorithm.impl.SpelAlgorithm;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * spel、ognl
 */
@MetaInfServices(Module.class)
@Information(id = "expression-algorithm", author = "jrasp")
public class ExpressionAlgorithm extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private String metaInfo;

    private volatile SpelAlgorithm spelAlgorithm;

    private volatile OgnlAlgorithm ognlAlgorithm;

    @Override
    public boolean update(Map<String, String> configMaps) {
        spelAlgorithm = new SpelAlgorithm(logger, raspConfig, configMaps, metaInfo);
        ognlAlgorithm = new OgnlAlgorithm(logger, raspConfig, configMaps, metaInfo);
        algorithmManager.register(spelAlgorithm);
        algorithmManager.register(ognlAlgorithm);
        return false;
    }

    @Override
    public void loadCompleted() {
        this.spelAlgorithm = new SpelAlgorithm(logger, raspConfig, metaInfo);
        this.ognlAlgorithm = new OgnlAlgorithm(logger, raspConfig, metaInfo);
        algorithmManager.register(spelAlgorithm);
        algorithmManager.register(ognlAlgorithm);
    }

    @Override
    public void onUnload() throws Throwable {
        if (spelAlgorithm != null) {
            algorithmManager.destroy(spelAlgorithm);
            spelAlgorithm = null;
        }
        if (ognlAlgorithm != null) {
            algorithmManager.destroy(ognlAlgorithm);
            ognlAlgorithm = null;
        }
        logger.info("expression-algorithm onUnload success.");
    }
}
