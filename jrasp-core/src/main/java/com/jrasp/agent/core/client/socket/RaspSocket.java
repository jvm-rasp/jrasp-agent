package com.jrasp.agent.core.client.socket;

import com.jrasp.agent.core.client.packet.Packet;
import com.jrasp.agent.core.client.packet.PacketType;

/**
 * @author jrasp
 * @since 2023/05/27
 */
public interface RaspSocket {

    /**
     * 连接服务器
     */
    void connect() throws Exception;

    void write(String msg, PacketType t)throws Exception;

    /**
     * 接受指令/参数
     */
    Packet read() throws Exception;

    boolean isClosed();
    /**
     * 关闭socket
     */
    void close() throws Exception;

}
