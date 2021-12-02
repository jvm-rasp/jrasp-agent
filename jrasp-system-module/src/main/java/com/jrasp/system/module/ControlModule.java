package com.jrasp.system.module;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jrasp.api.ConfigInfo;
import com.jrasp.api.Information;
import com.jrasp.api.Module;
import com.jrasp.api.Resource;
import com.jrasp.api.annotation.Command;
import com.jrasp.api.model.RestResultUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

@MetaInfServices(Module.class)
@Information(id = "control", version = "0.0.1", author = "jrasp")
public class ControlModule implements Module {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ConfigInfo configInfo;

    // 卸载jvm-rasp
    private void uninstall() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> classOfAgentLauncher = getClass().getClassLoader()
                .loadClass("com.jrasp.agent.AgentLauncher");

        MethodUtils.invokeStaticMethod(
                classOfAgentLauncher,
                "uninstall",
                configInfo.getNamespace()
        );
    }

    @Command("shutdown")
    public void shutdown(final PrintWriter writer) {
        logger.info("prepare to shutdown jvm-rasp[{}].", configInfo.getNamespace());
        // 关闭HTTP服务器
        final Thread shutdownJvmRaspHook = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uninstall();
                } catch (Throwable cause) {
                    logger.warn("shutdown jvm-rasp failed.", cause);
                }
            }
        }, "shutdown-jvm-rasp-hook");
        shutdownJvmRaspHook.setDaemon(true);

        // 在卸载自己之前，先向这个世界发出最后的呐喊吧！
        writer.println(JSONObject.toJSONString(RestResultUtils.success("shutdown success", null), SerializerFeature.PrettyFormat));
        writer.flush();
        writer.close();

        shutdownJvmRaspHook.start();
    }

}
