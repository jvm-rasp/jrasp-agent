package com.jrasp.agent.module.jndi.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.AttackInfo;
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
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> requestContext;

    @Override
    public void loadCompleted() {
        jndiHook();
    }

    private static final String TYPE = "jndi";

    private volatile Boolean disable = false;

    // 明确为攻击,可以直接阻断
    private volatile Integer jndiBlackListAction = 0;

    private volatile String[] dangerProtocol = new String[]{"ldap://", "rmi://"};

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        this.jndiBlackListAction = ParamSupported.getParameter(configMaps, "jndi_black_list_action", Integer.class, jndiBlackListAction);
        this.dangerProtocol = ParamSupported.getParameter(configMaps, "danger_protocol", String[].class, dangerProtocol);
        return false;
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
                                        check(requestContext.get(), lookupUrl);
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

    // 拦截所有jndi调用
    public void check(Context context, Object... parameters) throws Exception {
        if (this.jndiBlackListAction > -1) {
            if (parameters != null && parameters.length >= 1) {
                String lookupUrl = (String) parameters[0];
                if (hasDangerProtocol(lookupUrl)) {
                    boolean block = jndiBlackListAction == 1;
                    AttackInfo attackInfo = new AttackInfo(context, lookupUrl, block, TYPE, "danger jndi url", "", 100);
                    logger.attack(attackInfo);
                    if (block) {
                        ProcessControlException.throwThrowsImmediately(new RuntimeException("jndi inject block by rasp."));
                    }
                }
            }
        }
    }

    private boolean hasDangerProtocol(String url) {
        for (String protocol : dangerProtocol) {
            if (url.contains(protocol)) {
                return true;
            }
        }
        return false;
    }
}
