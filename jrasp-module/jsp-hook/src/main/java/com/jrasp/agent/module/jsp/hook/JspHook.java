package com.jrasp.agent.module.jsp.hook;

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
@Information(id = "jsp-hook")
public class JspHook implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String JSP_COMPILE = "jsp-compile";
    private final static String JSTL_IMPORT = "jstl-import";

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("org/apache/jasper/JspCompilationContext")
                        .onMethod("compile()V"
                                , new compileListener()))
                .onClass(new ClassMatcher("org/apache/taglibs/standard/tag/common/core/ImportSupport")
                        .onMethod("targetUrl()Ljava/lang/String;"
                                , new importListener()))
                .build();
    }

    public class compileListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }

            algorithmManager.doCheck(JSP_COMPILE, context.get());
        }
    }

    public class importListener extends AdviceListener {

        @Override
        protected void after(Advice advice) throws Throwable {
            if (disable) {
                return;
            }

            String url = (String) advice.getReturnObj();
            algorithmManager.doCheck(JSTL_IMPORT, context.get(), url);
        }
    }

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }
}
