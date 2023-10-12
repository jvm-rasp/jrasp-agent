package com.jrasp.agent.core.task;

import com.jrasp.agent.core.newlog.LogUtil;

/**
 * rasp task 异常统一处理
 * @author jrasp
 */
public abstract class AbstractRaspTask implements Runnable {

    private int errorCnt = 0;

    /**
     * RaspTask 调用频率很高，错误日志打印做一个采样，避免错误日志太多
     *
     * @param t 异常
     */
    public void handleError(Throwable t) {
        if (++errorCnt % 100 == 0) {
            LogUtil.error("handle rasp task error, errorCnt: " + errorCnt, t);
        }
    }

}
