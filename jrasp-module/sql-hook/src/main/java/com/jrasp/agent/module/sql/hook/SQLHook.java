package com.jrasp.agent.module.sql.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.util.Map;

@MetaInfServices(Module.class)
@Information(id = "sql-hook")
public class SQLHook implements Module, LoadCompleted {

    @RaspResource
    private RaspLog LOGGER;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    /**
     * hook开关，默认开启，可以在管理端统一配置
     */
    public static volatile Boolean disable = false;

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        new MySQLHook(moduleEventWatcher, algorithmManager, context);
        new OracleHook(moduleEventWatcher, algorithmManager, context);
        new SQLServerHook(moduleEventWatcher, algorithmManager, context);
    }
}
