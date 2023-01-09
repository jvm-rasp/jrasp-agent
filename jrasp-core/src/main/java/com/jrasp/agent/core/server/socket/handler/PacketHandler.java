package com.jrasp.agent.core.server.socket.handler;

import com.jrasp.agent.core.server.socket.handler.packet.PacketType;

/**
 * @author jrasp
 */
public interface PacketHandler {

    PacketType getType();

    String run(String data) throws Throwable;

}
