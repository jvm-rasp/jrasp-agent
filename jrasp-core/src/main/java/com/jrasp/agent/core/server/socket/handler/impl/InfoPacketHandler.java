package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.core.manager.RaspClassFileTransformer;
import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;

import java.util.Map;

import static com.jrasp.agent.core.server.socket.handler.packet.PacketType.INFO;

/**
 * 获取当前agent的信息
 *
 * @author jrasp
 */
public class InfoPacketHandler implements PacketHandler {

    public InfoPacketHandler() {
    }

    @Override
    public PacketType getType() {
        return INFO;
    }

    @Override
    public String run(String data) throws Throwable {
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, ClassMatcher> maps = RaspClassFileTransformer.INSTANCE.targetClazzMap;
        if (maps != null) {
            for (Map.Entry<String, ClassMatcher> entry : maps.entrySet()) {
                stringBuilder.append(entry.getValue().toString());
            }
        }
        return stringBuilder.toString();
    }
}
