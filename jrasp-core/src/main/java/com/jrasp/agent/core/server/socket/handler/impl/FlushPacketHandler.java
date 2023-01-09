package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;
import com.jrasp.agent.core.util.string.RaspStringUtils;

import static com.jrasp.agent.core.server.socket.handler.packet.PacketType.FLUSH;

/**
 * 刷新命令
 *
 * @author jrasp
 */
public class FlushPacketHandler implements PacketHandler {

    private final DefaultCoreModuleManager coreModuleManager;

    public FlushPacketHandler(DefaultCoreModuleManager coreModuleManager) {
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public PacketType getType() {
        return FLUSH;
    }

    @Override
    public String run(String data) throws Throwable {
        // true: 强制刷新
        // false: 软刷新
        boolean isForceFlush = false;
        if (RaspStringUtils.isNotBlank(data) && "true".equals(data)) {
            isForceFlush = true;
        }
        coreModuleManager.flush(isForceFlush);
        return "success";
    }
}
