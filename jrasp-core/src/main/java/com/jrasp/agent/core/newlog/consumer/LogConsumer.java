package com.jrasp.agent.core.newlog.consumer;

/**
 *  @author jrasp
 */
public interface LogConsumer {

    /**
     * 消费队列，写入文件、socket和console
     *
     * @param msg
     */
    void consumer(String msg) throws Exception;

    boolean isEnable();

    void setEnable(boolean enable);

    boolean isAvailable();

    void setAvailable(boolean available);

    void close();
}
