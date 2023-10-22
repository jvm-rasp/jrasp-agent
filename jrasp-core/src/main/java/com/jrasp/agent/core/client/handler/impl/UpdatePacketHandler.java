package com.jrasp.agent.core.client.handler.impl;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.client.handler.PacketHandler;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.client.packet.PacketType;
import com.jrasp.agent.core.util.string.RaspStringUtils;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jrasp.agent.core.client.packet.PacketType.MODULE_CONFIG;

/**
 * @author jrasp
 */
public class UpdatePacketHandler implements PacketHandler {

    private static final Logger LOGGER = Logger.getLogger(UpdatePacketHandler.class.getName());

    private final DefaultCoreModuleManager coreModuleManager;

    public UpdatePacketHandler(DefaultCoreModuleManager coreModuleManager) {
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public PacketType getType() {
        return MODULE_CONFIG;
    }

    @Override
    public String run(String data) throws Throwable {
        // 模块名称:k1=v1;k2=v2;k2=v21,v22,v23;
        LOGGER.log(Level.CONFIG, "data:{0}", data);
        if (RaspStringUtils.isNotBlank(data)) {
            String[] moduleAndValue = data.split(":", 2);
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
                String[] kvArray = parameters.split(";");
                Map<String, String> parametersMap = new HashMap<String, String>();
                for (String kv : kvArray) {
                    String[] kAndV = kv.split("=", 2);
                    if (kAndV.length == 2) {
                        parametersMap.put(kAndV[0], URLDecoder.decode(kAndV[1], "UTF-8"));
                    } else {
                        throw new RuntimeException("update module config failed. split kv array length not equal to 2, kv: " + kv);
                    }
                }

                // bugfix 输入参数存在不确定性字符，字符串转为map之后，需要校验转换的结果是否正确
                StringBuilder resultStr = new StringBuilder();
                resultStr.append(moduleName + ":");
                for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
                    resultStr.append(entry.getKey());
                    resultStr.append("=");
                    resultStr.append(entry.getValue());
                    resultStr.append(";");
                }
                assert data.equals(resultStr.toString());

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
