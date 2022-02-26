package com.jrasp.api.algorithm;

import java.util.HashMap;

public interface AlgorithmManager {
    /**
     * @param algorithm 算法对象
     * @return 注册是否成功
     */
    boolean register(Algorithm algorithm);

    /**
     * @param algorithm 算法对象
     * @return 销毁是否成功
     */
    boolean destroy(Algorithm algorithm);

    /**
     * 安全检测算法
     *
     * @param type       攻击类型
     * @param httpInfo   http信息
     * @param parameters 参数
     * @return 检测结果
     */
    boolean doCheck(String type, HashMap<String, Object> httpInfo, Object... parameters) throws Exception;

}
