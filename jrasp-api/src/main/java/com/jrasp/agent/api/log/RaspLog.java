package com.jrasp.agent.api.log;

import com.jrasp.agent.api.request.AttackInfo;

/**
 * @author jrasp
 */
public interface RaspLog {

    /**
     * 记录攻击日志
     *
     * @param attackInfo
     */
    void attack(AttackInfo attackInfo);

    void info(String message);

    void warning(String message);

    void error(String message);

    void error(String message, Throwable t);
}
