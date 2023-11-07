package com.jrasp.agent.core.client.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * @author jrasp
 */
public enum Codec {

    /**
     * 单例
     */
    INSTANCE;

    /**
     * 编码并输出
     *
     * @param out    输出流
     * @param packet 消息
     * @throws Exception 异常
     */
    // |<-type(1byte)->|<----bodysize(4byte or int)---->|<----------------------------------body(bytes)------------------->|
    public void encode(DataOutputStream out, Packet packet) throws Exception {
        String params = packet.getData();
        if (params != null && params.length() > 0) {
            PacketType packetType = packet.getType();
            out.writeByte(packetType.getValue());
            byte[] bytes = params.getBytes(Constant.DEFAULT_CHARSET);
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    // |<-type(1byte)->|<----bodysize(4byte or int)---->|<----------------------------------body(bytes)------------------->|
    public Packet decode(DataInputStream inputStream) throws Exception {
        Packet packet = new Packet();

        byte type = inputStream.readByte();
        PacketType packetType = PacketType.getByType(type);
        packet.setType(packetType);

        int bodySize = inputStream.readInt();
        packet.setBodySize(bodySize);

        byte[] bytes = new byte[bodySize];
        inputStream.readFully(bytes);
        String value = new String(bytes, Constant.DEFAULT_CHARSET);
        packet.setData(value);
        return packet;
    }

}
