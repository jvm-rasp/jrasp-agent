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

    private final static String JSTL_IMPORT = "jstl-import";

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)

                // tomcat servlet/jakarta api
                // jetty/weblogic 也使用 org.apache.jasper
                // https://tomcat.apache.org/tomcat-9.0-doc/api/org/apache/jasper/runtime/HttpJspBase.html#service(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)
                // https://tomcat.apache.org/tomcat-11.0-doc/api/org/apache/jasper/runtime/HttpJspBase.html#service(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse) 
                .onClass(new ClassMatcher("org/apache/jasper/runtime/HttpJspBase")
                        .onMethod("service(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"
                                , new HttpJspPageServiceListener())
                        .onMethod("service(Ljakarta/servlet/http/HttpServletRequest;Ljakarta/servlet/http/HttpServletResponse;)V"
                                , new HttpJspPageServiceListener())
                )
                .onClass(new ClassMatcher("org/apache/taglibs/standard/tag/common/core/ImportSupport")
                        .onMethod("targetUrl()Ljava/lang/String;"
                                , new ImportListener()))
                .build();
    }

    public class HttpJspPageServiceListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            context.get().setInJspContext(true);
        }

        @Override
        protected void afterReturning(Advice advice) throws Throwable {
            context.get().setInJspContext(false);
        }

    }

    public class ImportListener extends AdviceListener {

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
