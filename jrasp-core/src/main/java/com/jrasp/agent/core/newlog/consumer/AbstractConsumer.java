package com.jrasp.agent.core.newlog.consumer;

/**
 * @author jrasp
 */
public abstract class AbstractConsumer implements LogConsumer {

    /**
     * 配置是否开启
     */
    protected volatile boolean enable = true;

    /**
     * 是否可用
     */
    protected volatile boolean available = true;
    /**
     * 是否可用/开启
     *
     * @return
     */
    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void setAvailable(boolean available) {
        this.available = available;
    }
}
