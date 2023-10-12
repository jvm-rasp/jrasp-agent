package com.jrasp.agent.core.client.handler.impl;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.client.handler.PacketHandler;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.client.packet.PacketType;

/**
 * 卸载指定模块
 *
 * @author jrasp
 */
public class UnloadModuleHandler implements PacketHandler {

    private final DefaultCoreModuleManager coreModuleManager;

    public UnloadModuleHandler(DefaultCoreModuleManager coreModuleManager) {
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public PacketType getType() {
        return PacketType.UNLOAD;
    }

    @Override
    public String run(String/*moduleId*/ data) throws Throwable {
        for (CoreModule coreModule : coreModuleManager.list()) {
            Module module = coreModule.getModule();
            final Information moduleInfo = module.getClass().getAnnotation(Information.class);
            if (moduleInfo.id().equals(data)) {
                coreModuleManager.unload(coreModule, true);
            }
        }
        return "success";
    }

}
