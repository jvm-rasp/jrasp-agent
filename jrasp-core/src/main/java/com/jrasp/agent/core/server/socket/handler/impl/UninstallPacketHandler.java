package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;

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
        checkReflect();
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

    // 禁止反射
    private void checkReflect() {
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        for (StackTraceElement s : stackTraces) {
            String className = s.getClassName();
            if (className.contains("java.lang.reflect.") || className.contains("jdk.internal.reflect.")) {
                throw new SecurityException("Reflective call detected in the stack trace.");
            }
        }
    }
}
