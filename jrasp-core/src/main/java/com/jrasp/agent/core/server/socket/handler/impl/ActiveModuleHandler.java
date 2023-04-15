package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;

/**
 * 激活指定模块
 * @author jrasp
 */
public class ActiveModuleHandler implements PacketHandler {

    private final DefaultCoreModuleManager coreModuleManager;

    public ActiveModuleHandler(DefaultCoreModuleManager coreModuleManager) {
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public PacketType getType() {
        return PacketType.ACTIVE;
    }

    @Override
    public String run(String data) throws Throwable {
        for (CoreModule coreModule : coreModuleManager.list()) {
            Module module = coreModule.getModule();
            final Information moduleInfo = module.getClass().getAnnotation(Information.class);
            if (moduleInfo.id().equals(data)) {
                coreModuleManager.active(coreModule);
            }
        }
        return "success";
    }

}
