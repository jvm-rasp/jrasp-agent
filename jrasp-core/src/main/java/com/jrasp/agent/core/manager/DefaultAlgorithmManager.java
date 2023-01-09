package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.request.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 算法插件管理
 *
 * @author jrasp
 */
public class DefaultAlgorithmManager implements AlgorithmManager {

    private final static Logger logger = Logger.getLogger(DefaultAlgorithmManager.class.getName());

    public static final DefaultAlgorithmManager instance = new DefaultAlgorithmManager();

    private static Map<String, Algorithm> algorithmMaps = new ConcurrentHashMap<String, Algorithm>(128);

    @Override
    public boolean register(Algorithm algorithm) {
        String type = algorithm.getType();
        algorithmMaps.put(type, algorithm);
        logger.log(Level.INFO, "register algorithm module: {0}", type);
        return true;
    }

    @Override
    public boolean register(Algorithm... algorithms) {
        for (Algorithm algorithm : algorithms) {
            register(algorithm);
        }
        return true;
    }

    @Override
    public boolean destroy(Algorithm algorithm) {
        String type = algorithm.getType();
        algorithmMaps.remove(type);
        logger.log(Level.INFO, "destroy algorithm module: {0}", type);
        return true;
    }

    @Override
    public void doCheck(String type, Context context, Object... parameters) throws Exception {
        Algorithm algorithm = algorithmMaps.get(type);
        if (algorithm == null) {
            return;
        }
        algorithm.check(context, parameters);
    }
}
