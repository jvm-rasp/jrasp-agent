package com.jrasp.agent.api.algorithm;

import com.jrasp.agent.api.request.Context;

/**
 * @author jrasp
 */
public interface Algorithm {

    /**
     * @return 算法/攻击类型  rce、sql、file-delete、file-read
     */
    String getType();

    /**
     * 安全检测算法
     *
     * @param context    http信息
     * @param parameters 参数
     */
    void check(Context context, Object... parameters) throws Exception;

    /**
     * @return 算法描述
     */
    String getDescribe();
}
