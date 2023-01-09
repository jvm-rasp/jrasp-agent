package com.jrasp.agent.core.server.socket.handler.impl;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.server.socket.handler.PacketHandler;
import com.jrasp.agent.core.server.socket.handler.packet.PacketType;
import com.jrasp.agent.core.util.FeatureCodec;
import com.jrasp.agent.core.util.string.RaspStringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jrasp.agent.core.server.socket.handler.packet.PacketType.UPDATE;

/**
 * @author jrasp
 */
public class UpdatePacketHandler implements PacketHandler {

    private static final Logger LOGGER = Logger.getLogger(UpdatePacketHandler.class.getName());

    private final DefaultCoreModuleManager coreModuleManager;

    private static final FeatureCodec kv = new FeatureCodec(';', '=');

    public UpdatePacketHandler(DefaultCoreModuleManager coreModuleManager) {
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public PacketType getType() {
        return UPDATE;
    }

    @Override
    public String run(String data) throws Throwable {
        // 模块名称:k1=v1;k2=v2;k2=v21,v22,v23;
        LOGGER.log(Level.INFO, "data:{0}", data);
        if (RaspStringUtils.isNotBlank(data)) {
            String[] moduleAndValue = data.split(":",2);
            if (moduleAndValue.length == 2) {
                String moduleName = moduleAndValue[0].trim();
                String parameters = moduleAndValue[1].trim();
                CoreModule coreModule = coreModuleManager.get(moduleName);
                if (coreModule == null) {
                    return "coreModule is null," + moduleName;
                }
                Module module = coreModule.getModule();
                if (module == null) {
                    return "module is null," + moduleName;
                }
                Map<String, String> parametersMap = kv.toMap(parameters);
                Method update = module.getClass().getDeclaredMethod("update", Map.class);
                Boolean result = (Boolean) update.invoke(module, parametersMap);
                return result.toString();
            } else {
                return "split parameters error. keyAndValue length: " + moduleAndValue.length + "; data: " + data;
            }
        } else {
            return "parameters is blank";
        }
    }
}
