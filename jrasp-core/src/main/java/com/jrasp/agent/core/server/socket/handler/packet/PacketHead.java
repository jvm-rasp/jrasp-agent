package com.jrasp.agent.core.server.socket.handler.packet;

/**
 * @author jrasp
 */
public class PacketHead {

    /**
     * 3
     */
    private byte[] magic;

    /**
     * 1
     */
    private int version;

    /**
     * 4
     */
    private int bodySize;

    /**
     * 8
     */
    private long timeStamp;

    /**
     * 1
     */
    private PacketType type;

    /**
     * 128
     */
    private byte[] signature;

    public PacketHead() {
    }

    public PacketHead(PacketType packetType) {
        this(packetType, Constant.PROTOCOL_VERSION);
    }

    private PacketHead(PacketType packetType, byte packetVersion) {
        type = packetType;
        signature = new byte[128];
        version = packetVersion;
        timeStamp = System.currentTimeMillis();
    }

    public byte[] getMagic() {
        return magic;
    }

    public void setMagic(byte[] magic) {
        this.magic = magic;
    }

    public int getBodySize() {
        return bodySize;
    }

    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}
