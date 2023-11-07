package com.jrasp.agent.core.client.packet;

/**
 * @author jrasp
 */
public enum PacketType {

    // --------------------- agent ---------------------
    /**
     * 更新agent参数
     */
    AGENT_CONFIG((byte) 0x01),

    /**
     * 卸载agent
     */
    AGENT_UNINSTALL((byte) 0x02),

    /**
     * 获取agent信息
     */
    AGENT_INFO((byte) 0x03),

    /**
     * agent日志
     */
    AGENT_LOG((byte) 0x04),

    // --------------------- module ---------------------
    /**
     * 卸载module
     */
    MODULE_UNINSTALL((byte) 0x20),

    /**
     * 更新module参数
     */
    MODULE_CONFIG((byte) 0x21),

    /**
     * 刷新module
     */
    MODULE_FLUSH((byte) 0x22),

    /**
     * 激活模块
     */
    MODULE_ACTIVE((byte) 0x23),

    /**
     * 冻结模块
     */
    MODULE_FROZEN((byte) 0x24),

    // --------------------- other ---------------------
    /**
     * 命令的返回
     */
    COMMAND_RESPONSE((byte) 0x30);

    private final byte value;

    PacketType(byte value) {
        this.value = value;
    }

    public static PacketType getByType(byte type) {
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
