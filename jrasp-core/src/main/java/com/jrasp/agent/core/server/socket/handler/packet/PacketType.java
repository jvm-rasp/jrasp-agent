package com.jrasp.agent.core.server.socket.handler.packet;

/**
 * @author jrasp
 */
public enum PacketType {

    /**
     * 获取信息
     */
    INFO(0x01),

    /**
     * 卸载rasp
     */
    UNINSTALL(0x02),

    /**
     * 刷新模块
     */
    FLUSH(0x03),

    /**
     * 错误
     */
    ERROR(0x04),

    /**
     * 更新参数
     */
    UPDATE(0x05),

    /**
     * 卸载模块
     */
    UNLOAD(0x06),

    /**
     * 激活模块
     */
    ACTIVE(0x07),

    /**
     * 冻结模块
     */
    FROZEN(0x08);

    private final int value;

    PacketType(int value) {
        this.value = value;
    }

    public static PacketType getByType(int type) {
        for (PacketType packetType : values()) {
            if (packetType.getValue() == type) {
                return packetType;
            }
        }
        throw new IllegalArgumentException("unexpected packet body type:" + type);
    }

    public int getValue() {
        return value;
    }
}
