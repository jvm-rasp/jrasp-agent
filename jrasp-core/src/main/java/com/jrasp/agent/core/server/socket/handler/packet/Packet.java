package com.jrasp.agent.core.server.socket.handler.packet;

/**
 * @author jrasp
 */
public class Packet {

    private PacketHead packetHead;

    private String data;

    public Packet() {
    }

    public Packet(PacketType packetType, String data) {
        packetHead = new PacketHead(packetType);
        this.data = data;
    }

    public Packet(PacketHead packetHead, String data) {
        this.packetHead = packetHead;
        this.data = data;
    }

    public int getBodySize() {
        return packetHead.getBodySize();
    }

    public PacketType getType() {
        return packetHead.getType();
    }

    public PacketHead getPacketHead() {
        return packetHead;
    }

    public void setPacketHead(PacketHead packetHead) {
        this.packetHead = packetHead;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
