package com.jrasp.agent.api.algorithm;

import com.jrasp.agent.api.request.Context;

/**
 * @author jrasp
 */
public interface AlgorithmManager {
    /**
     * @param algorithm 算法对象
     * @return 注册是否成功
     */
    boolean register(Algorithm algorithm);

    /**
     * @param algorithms 算法对象
     * @return 注册是否成功
     */
    boolean register(Algorithm... algorithms);

    /**
     * @param algorithm 算法对象
     * @return 销毁是否成功
     */
    boolean destroy(Algorithm algorithm);

    /**
     * 安全检测算法
     *
     * @param type       攻击类型
     * @param type       常量
     * @param context    http信息
     * @param parameters 参数
     */
    void doCheck(String type, Context context, Object... parameters) throws Exception;


}
