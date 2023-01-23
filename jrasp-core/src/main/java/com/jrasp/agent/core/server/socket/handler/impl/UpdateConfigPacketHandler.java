package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.core.CoreConfigure;
import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;
import com.jrasp.agent.core.util.string.RaspStringUtils;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jrasp.agent.core.server.socket.handler.packet.PacketType.CONFIG;

/**
 * @author jrasp
 * 更新全局参数
 */
public class UpdateConfigPacketHandler implements PacketHandler {

    private static final Logger LOGGER = Logger.getLogger(UpdateConfigPacketHandler.class.getName());

    public UpdateConfigPacketHandler() {
    }

    @Override
    public PacketType getType() {
        return CONFIG;
    }

    @Override
    public String run(String config) throws Throwable {
        // k1=v1;k2=v2;k2=v21,v22,v23;
        LOGGER.log(Level.INFO, "config:{0}", config);
        if (RaspStringUtils.isNotBlank(config)) {
            String[] kAndvArrays = config.split(";");
            Class<?> clazz = CoreConfigure.getInstance().getClass();
            for (String kvString : kAndvArrays) {
                if (RaspStringUtils.isNotBlank(kvString)) {
                    String[] kv = kvString.split("=", 2);
                    if (kv != null && kv.length == 2) {
                        // TODO 仅支持字符串类型字段
                        String fieldName = kv[0].trim();
                        String value = kv[1].trim();
                        Field field = clazz.getDeclaredField(fieldName);
                        field.set(CoreConfigure.getInstance(), value);
                    } else {
                        throw new RuntimeException("update agent config failed. split kv array length not equal to 2, kv: " + kv);
                    }
                }
            }
            return "success";
        } else {
            return "parameters is blank";
        }
    }
}
