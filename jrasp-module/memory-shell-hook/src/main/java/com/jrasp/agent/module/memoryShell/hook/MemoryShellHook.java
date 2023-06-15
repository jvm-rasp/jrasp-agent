package com.jrasp.agent.module.memoryShell.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.util.Map;

@MetaInfServices(Module.class)
@Information(id = "memory-shell-hook")
public class MemoryShellHook implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String TYPE = "内存马注入攻击";

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)
                // Filter类型内存马
                .onClass(new ClassMatcher("org/apache/catalina/core/StandardContext")
                        .onMethod(new String[]{
                                        // Filter类型内存马
                                        "addFilterDef(Lorg/apache/tomcat/util/descriptor/web/FilterDef;)V",
                                        "addFilterMapBefore(Lorg/apache/tomcat/util/descriptor/web/FilterMap;)V",
                                        "addFilterMap(Lorg/apache/tomcat/util/descriptor/web/FilterMap;)V",
                                        // Servlet类型内存马
                                        "addChild(Lorg/apache/catalina/Container;)V",
                                        "addServletMapping(Ljava/lang/String;Ljava/lang/String;)V",
                                        // Listener类型内存马
                                        "addApplicationEventListener(Ljava/lang/Object;)V"
                                }
                                , new InjectListener()))
                .build();
    }

    public class InjectListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }

            algorithmManager.doCheck(TYPE, context.get());
        }
    }
}
