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

    private static final byte[] MAGIC_BYTES = {88, 77, 68};

    /**
     * 编码并输出
     *
     * @param out    输出流
     * @param packet 消息
     * @throws Exception 异常
     */
    public void encode(DataOutputStream out, Packet packet) throws Exception {
        int bodySize = packet.getData().length();
        PacketType packetType = packet.getType();
        out.write(MAGIC_BYTES);
        out.write(packet.getPacketHead().getVersion());
        out.write(packetType.getValue());
        out.writeInt(bodySize);
        out.writeLong(packet.getPacketHead().getTimeStamp());
        out.write(packet.getPacketHead().getSignature());
        String params = packet.getData();
        if (params != null && params.length() > 0) {
            out.write(params.getBytes(Constant.DEFAULT_CHARSET));
        }
    }

    // |<---magic(3byte)--->|<-version(1byte)->|<-type(1byte)->|<----bodysize---->|<----time(8bytes)---->|
    // |<---sig(128)------------------->|<----------------------------------body------------------->|
    public Packet decode(DataInputStream inputStream) throws Exception {
        byte[] magicHead = new byte[3];
        inputStream.readFully(magicHead);
        verifyMagicHead(magicHead);

        int version = inputStream.read();
        PacketHead packetHead = new PacketHead();
        packetHead.setVersion(version);
        verifyVersion(version);

        int type = inputStream.read();

        int bodySize = inputStream.readInt();
        packetHead.setBodySize(bodySize);

        byte[] timeBytes = new byte[8];
        inputStream.readFully(timeBytes);
        packetHead.setTimeStamp(toLong(timeBytes));

        byte[] sig = new byte[128];
        inputStream.readFully(sig);

        PacketType packetType = PacketType.getByType(type);
        packetHead.setType(packetType);

        Packet packet = new Packet();
        packet.setPacketHead(packetHead);

        byte[] bytes = new byte[bodySize];
        inputStream.readFully(bytes);
        String value = new String(bytes, Constant.DEFAULT_CHARSET);
        packet.setData(value);
        return packet;
    }

    private void verifyVersion(int version) throws Exception {
        if (version != Constant.PROTOCOL_VERSION) {
            throw new RuntimeException(String.format("unsupported protocol version:%d, current protocol version:%d", version, Constant.PROTOCOL_VERSION));
        }
    }

    private void verifyMagicHead(byte[] input) throws Exception {
        if (input.length != MAGIC_BYTES.length) {
            throw new RuntimeException("invalid magic head");
        }
        for (int i = 0; i < MAGIC_BYTES.length; i++) {
            if (MAGIC_BYTES[i] != input[i]) {
                throw new RuntimeException("invalid magic head");
            }
        }
    }

    private long toLong(byte[] readBuffer) {
        return (((long) readBuffer[0] << 56) + ((long) (readBuffer[1] & 255) << 48) + ((long) (readBuffer[2] & 255) << 40) + ((long) (readBuffer[3] & 255) << 32) + ((long) (readBuffer[4] & 255) << 24) + ((readBuffer[5] & 255) << 16) + ((readBuffer[6] & 255) << 8) + ((readBuffer[7] & 255)));
    }
}
