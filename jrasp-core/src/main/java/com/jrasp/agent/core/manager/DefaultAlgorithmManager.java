package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.core.newlog.LogUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算法插件管理
 *
 * @author jrasp
 */
public class DefaultAlgorithmManager implements AlgorithmManager {

    public static final DefaultAlgorithmManager instance = new DefaultAlgorithmManager();

    private static Map<String, Algorithm> algorithmMaps = new ConcurrentHashMap<String, Algorithm>(128);

    @Override
    public boolean register(Algorithm algorithm) {
        String type = algorithm.getType();
        algorithmMaps.put(type, algorithm);
        LogUtil.info("register algorithm module: " + type);
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
        LogUtil.info("destroy algorithm module: " + type);
        return true;
    }

    @Override
    public void doCheck(String type, Context context, Object... parameters) throws Exception {
        // 对于hook和algorithm不在一个jar包的场景，全局检测开关有效
        // 约定：如果在同一个jar包中，需要单独增加全局关闭开关
        RaspConfig raspConfig = RaspConfigImpl.getInstance();
        if (raspConfig.isCheckDisable()) {
            return;
        }
        Algorithm algorithm = algorithmMaps.get(type);
        if (algorithm == null) {
            return;
        }
        algorithm.check(context, parameters);
    }
}
