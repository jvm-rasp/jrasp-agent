package com.jrasp.api.algorithm;

import java.util.ArrayList;
import java.util.HashMap;

public interface Algorithm {

    /**
     * @return 算法名称
     */
    String getName();

    /**
     * @return 算法/攻击类型  rce、file_delete、file_read
     */
    String getType();

    /**
     * 安全检测算法
     * @param parameters 参数
     * @param stack 调用栈
     * @param httpInfo http信息
     * @return 检测结果
     */
    boolean check(String[] parameters, ArrayList<String> stack, HashMap<String,Object> httpInfo);

    /**
     * @return 算法描述
     */
    String getDescribe();
}
