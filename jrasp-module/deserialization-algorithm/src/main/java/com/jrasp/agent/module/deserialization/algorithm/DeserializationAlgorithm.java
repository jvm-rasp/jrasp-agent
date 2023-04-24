package com.jrasp.agent.module.deserialization.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.module.deserialization.algorithm.impl.JsonAlgorithm;
import com.jrasp.agent.module.deserialization.algorithm.impl.OisAlgorithm;
import com.jrasp.agent.module.deserialization.algorithm.impl.XmlAlgorithm;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * @author jrasp
 * json、yaml、ois、xml
 */
@MetaInfServices(Module.class)
@Information(id = "deserialization-algorithm", author = "jrasp")
public class DeserializationAlgorithm extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private String metaInfo;

    private OisAlgorithm oisAlgorithm;

    private JsonAlgorithm jsonAlgorithm;

    private XmlAlgorithm xmlAlgorithm;

    @Override
    public boolean update(Map<String, String> configMaps) {
        oisAlgorithm = new OisAlgorithm(logger, configMaps, metaInfo);
        jsonAlgorithm = new JsonAlgorithm(logger, configMaps, metaInfo);
        xmlAlgorithm = new XmlAlgorithm(logger, configMaps, metaInfo);
        algorithmManager.register(oisAlgorithm);
        algorithmManager.register(jsonAlgorithm);
        algorithmManager.register(xmlAlgorithm);
        return true;
    }

    @Override
    public void loadCompleted() {
        oisAlgorithm = new OisAlgorithm(logger, metaInfo);
        jsonAlgorithm = new JsonAlgorithm(logger, metaInfo);
        xmlAlgorithm = new XmlAlgorithm(logger, metaInfo);
        algorithmManager.register(oisAlgorithm);
        algorithmManager.register(jsonAlgorithm);
        algorithmManager.register(xmlAlgorithm);
    }

    @Override
    public void onUnload() throws Throwable {
        if (oisAlgorithm != null) {
            algorithmManager.destroy(oisAlgorithm);
            oisAlgorithm = null;
        }
        if (jsonAlgorithm != null) {
            algorithmManager.destroy(jsonAlgorithm);
            jsonAlgorithm = null;
        }
        if (xmlAlgorithm != null) {
            algorithmManager.destroy(xmlAlgorithm);
            xmlAlgorithm = null;
        }
        logger.info("deserialization-algorithm onUnload success.");
    }
}
