package com.jrasp.agent.module.jndi.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * @author jrasp
 * 2022-11-20 测试通过
 */
@MetaInfServices(Module.class)
@Information(id = "jndi-hook")
public class JndiHook implements Module, LoadCompleted {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> requestContext;

    @RaspResource
    private String metaInfo;

    @Override
    public void loadCompleted() {
        jndiHook();
    }

    private static final String TYPE = "jndi";

    private volatile Boolean disable = false;

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    public void jndiHook() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * @see com.sun.jndi.toolkit.url.GenericURLContext#lookup(String)
                 * @see com.sun.jndi.toolkit.url.GenericURLContext#lookupLink(String)
                 */
                .onClass(new ClassMatcher("com/sun/jndi/toolkit/url/GenericURLContext")
                        .onMethod(
                                new String[]{
                                        "lookup(Ljava/lang/String;)Ljava/lang/Object;",
                                        "lookupLink(Ljava/lang/String;)Ljava/lang/Object;"
                                },
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        String lookupUrl = (String) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(TYPE, requestContext.get(), lookupUrl);
                                    }

                                    @Override
                                    public void afterThrowing(Advice advice) throws Throwable {
                                        requestContext.remove();
                                    }
                                }
                        )
                )

                /**
                 * @see javax.naming.InitialContext
                 * 上下文关联，辅助hook类
                 *
                 */
                .onClass(new ClassMatcher("javax/naming/InitialContext")
                        .onMethod(
                                new String[]{
                                        "lookup(Ljava/lang/String;)Ljava/lang/Object;",
                                        "lookupLink(Ljava/lang/String;)Ljava/lang/Object;"
                                },
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        // 加标记
                                        requestContext.get().addMark(TYPE);
                                    }

                                    @Override
                                    public void after(Advice advice) throws Throwable {
                                        // 清除标记
                                        requestContext.get().remove(TYPE);
                                    }

                                    @Override
                                    public void afterThrowing(Advice advice) throws Throwable {
                                        requestContext.remove();
                                    }
                                }
                        )
                )
                .build();
    }
}
