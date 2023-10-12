package com.jrasp.agent.core.client.handler.impl;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.client.handler.PacketHandler;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.client.packet.PacketType;

/**
 * 冻结指定模块
 * @author jrasp
 */
public class FrozenModuleHandler implements PacketHandler {

    private final DefaultCoreModuleManager coreModuleManager;

    public FrozenModuleHandler(DefaultCoreModuleManager coreModuleManager) {
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public PacketType getType() {
        return PacketType.FROZEN;
    }

    @Override
    public String run(String data) throws Throwable {
        for (CoreModule coreModule : coreModuleManager.list()) {
            Module module = coreModule.getModule();
            final Information moduleInfo = module.getClass().getAnnotation(Information.class);
            if (moduleInfo.id().equals(data)) {
                coreModuleManager.frozen(coreModule, true);
            }
        }
        return "success";
    }

}
