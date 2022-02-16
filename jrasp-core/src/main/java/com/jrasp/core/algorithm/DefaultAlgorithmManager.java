package com.jrasp.core.algorithm;

import com.jrasp.api.algorithm.Algorithm;
import com.jrasp.api.algorithm.AlgorithmManager;
import com.jrasp.api.log.Log;
import com.jrasp.core.log.LogFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.jrasp.core.log.AgentLogIdConstant.AGENT_COMMON_LOG_ID;

public class DefaultAlgorithmManager implements AlgorithmManager {

    private final static Log logger = LogFactory.getLog(DefaultAlgorithmManager.class);

    public static final DefaultAlgorithmManager instance = new DefaultAlgorithmManager();

    /**
     * 读多写少
     */
    private static Map<String, CopyOnWriteArrayList<Algorithm>> algorithmMaps = new HashMap<String, CopyOnWriteArrayList<Algorithm>>(16);

    @Override
    public boolean register(Algorithm algorithm) {
        String type = algorithm.getType();
        if (algorithmMaps.containsKey(type)) {
            CopyOnWriteArrayList<Algorithm> algorithms = algorithmMaps.get(type);
            algorithms.add(algorithm);
            logger.info(AGENT_COMMON_LOG_ID, "add new algorithm", algorithm.getName());
        } else {
            CopyOnWriteArrayList<Algorithm> algorithms = new CopyOnWriteArrayList<Algorithm>();
            // todo 相同的id,保证只有一个
            algorithms.add(algorithm);
            algorithmMaps.put(type, algorithms);
        }
        return true;
    }

    @Override
    public boolean destroy(Algorithm algorithm) {
        String type = algorithm.getType();
        String name = algorithm.getName();
        CopyOnWriteArrayList<Algorithm> algorithms = algorithmMaps.get(type);
        int index = -1;
        for (int i = 0; i < algorithms.size() ; i++) {
            if (name.equals(algorithms.get(i).getName())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            logger.info(AGENT_COMMON_LOG_ID, "remove algorithm", algorithm.getName());
            algorithms.remove(index);
        }
        return true;
    }

    @Override
    public boolean check(String type, String[] parameters, ArrayList<String> stack, HashMap<String, Object> httpInfo) {
        CopyOnWriteArrayList<Algorithm> algorithms = algorithmMaps.get(type);
        for (int i = 0; i < algorithms.size(); i++) {
            Algorithm algorithm = algorithms.get(i);
            boolean check = algorithm.check(parameters, stack, httpInfo);
            if (check) {
                return true;
            }
        }
        return false;
    }
}
