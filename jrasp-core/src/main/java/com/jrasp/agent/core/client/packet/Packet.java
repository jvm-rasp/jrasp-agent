package com.jrasp.agent.core.client.packet;

/**
 * @author jrasp
 */
public class Packet {

    private int bodySize;

    private PacketType type;

    private String data;

    public Packet() {
    }

    public Packet(PacketType type, String data) {
        this.type = type;
        this.data = data;
    }

    public int getBodySize() {
        return bodySize;
    }

    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
