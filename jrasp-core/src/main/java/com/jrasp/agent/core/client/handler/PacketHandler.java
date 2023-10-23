package com.jrasp.agent.core.client.handler;

import com.jrasp.agent.core.client.packet.PacketType;

/**
 * @author jrasp
 */
public interface PacketHandler {

    PacketType getType();

    CommandResponse run(String data) throws Throwable;

}
