package com.jrasp.agent.core.task;

import com.jrasp.agent.core.client.packet.Codec;
import com.jrasp.agent.core.client.packet.Packet;
import com.jrasp.agent.core.client.packet.PacketType;
import com.jrasp.agent.core.client.socket.RaspSocket;
import com.jrasp.agent.core.client.handler.PacketHandler;

import static com.jrasp.agent.core.client.CoreClientImpl.handlerMap;

/**
 * @author jrasp
 */
public class PacketReadTask extends AbstractRaspTask {

    private final RaspSocket raspSocket;

    public PacketReadTask(RaspSocket raspSocket) {
        this.raspSocket = raspSocket;
    }

    @Override
    public void run() {
        try {
            if (raspSocket != null && !raspSocket.isClosed()) {
                Packet request = raspSocket.read();
                if (request == null) {
                    return;
                }
                PacketType type = request.getType();
                // 任务处理
                PacketHandler handler = handlerMap.get(type);
                if (handler == null) {
                    throw new IllegalArgumentException(String.format("no handle packet. packet type -> [%s]", type));
                }
                // 处理结果返回给daemon
                String responseData = handler.run(request.getData());
                raspSocket.write(responseData, PacketType.COMMAND_RESPONSE);
            }
        } catch (Throwable t) {
            handleError(t);
        }
    }
}
