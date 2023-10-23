package com.jrasp.agent.core.client.handler.impl;

import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.core.client.handler.CommandResponse;
import com.jrasp.agent.core.client.handler.PacketHandler;
import com.jrasp.agent.core.manager.RaspClassFileTransformer;
import com.jrasp.agent.core.client.packet.PacketType;

import java.util.Map;

import static com.jrasp.agent.core.client.packet.PacketType.*;

/**
 * 获取当前agent的信息
 *
 * @author jrasp
 */
public class AgentInfoPacketHandler implements PacketHandler {

    public AgentInfoPacketHandler() {
    }

    @Override
    public PacketType getType() {
        return AGENT_INFO;
    }

    @Override
    public CommandResponse run(String data) throws Throwable {
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, ClassMatcher> maps = RaspClassFileTransformer.INSTANCE.targetClazzMap;
        if (maps != null) {
            for (Map.Entry<String, ClassMatcher> entry : maps.entrySet()) {
                stringBuilder.append(entry.getValue().toString());
            }
        }
        return CommandResponse.ok(stringBuilder.toString(), getType());
    }
}
