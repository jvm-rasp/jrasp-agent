package com.jrasp.agent.api.event;

/**
 * 调用事件
 * JVM方法调用事件
 *
 * @author luanjia@taobao.com
 */
public abstract class Event {

    /**
     * 事件类型
     */
    public final Type type;

    /**
     * 构造调用事件
     *
     * @param type 事件类型
     */
    protected Event(Type type) {
        this.type = type;
    }

    /**
     * 事件枚举类型
     */
    public enum Type {

        BEFORE,

        RETURN,

        THROWS,

        IMMEDIATELY_RETURN,

        IMMEDIATELY_THROWS;

    }

}
