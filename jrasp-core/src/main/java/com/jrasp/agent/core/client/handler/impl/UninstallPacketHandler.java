package com.jrasp.agent.core.client.handler.impl;

import com.jrasp.agent.core.client.handler.PacketHandler;
import com.jrasp.agent.core.client.packet.PacketType;
import java.lang.reflect.Method;

/**
 * @author jrasp
 */
public class UninstallPacketHandler implements PacketHandler {

    @Override
    public PacketType getType() {
        return PacketType.UNINSTALL;
    }

    @Override
    public String run(String data) throws Throwable {
        /**
         * @see com.jrasp.agent.launcher110.AgentLauncher#uninstall(String)
         * TODO 这里存在不安全设计：反射卸载的接口在agent中，可以被非法调用
         */
        Class<?> clazz = getClass().getClassLoader().loadClass("com.jrasp.agent.launcher110.AgentLauncher");
        Method uninstallMethod = clazz.getDeclaredMethod("uninstall", String.class);
        uninstallMethod.setAccessible(true);
        uninstallMethod.invoke(null, "default");
        return "success";
    }
}
