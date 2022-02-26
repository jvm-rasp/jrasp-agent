package com.jrasp.core.algorithm;

import com.jrasp.api.algorithm.Algorithm;
import com.jrasp.api.algorithm.AlgorithmManager;
import com.jrasp.api.log.Log;
import com.jrasp.core.log.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jrasp.core.log.AgentLogIdConstant.AGENT_COMMON_LOG_ID;

public class DefaultAlgorithmManager implements AlgorithmManager {

    private final static Log logger = LogFactory.getLog(DefaultAlgorithmManager.class);

    public static final DefaultAlgorithmManager instance = new DefaultAlgorithmManager();

    private static Map<String, Algorithm> algorithmMaps = new ConcurrentHashMap<String, Algorithm>(16);

    @Override
    public boolean register(Algorithm algorithm) {
        String type = algorithm.getType();
        algorithmMaps.put(type, algorithm);
        logger.info(AGENT_COMMON_LOG_ID, "register algorithm module {}", type);
        return true;
    }

    @Override
    public boolean destroy(Algorithm algorithm) {
        String type = algorithm.getType();
        algorithmMaps.remove(type);
        logger.info(AGENT_COMMON_LOG_ID, "destroy algorithm module {}", type);
        return true;
    }

    @Override
    public boolean doCheck(String type, HashMap<String, Object> httpInfo, Object... parameters) throws Exception {
        Algorithm algorithm = algorithmMaps.get(type);
        return algorithm.check(httpInfo, parameters);
    }
}
