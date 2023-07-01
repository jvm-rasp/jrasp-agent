package com.jrasp.agent.module.shiro.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ProcessController;
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
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.kohsuke.MetaInfServices;

import java.util.*;

/**
 * shiro 默认密码防护
 * 由 @是小易呀、@hycsxs 提供
 */
@MetaInfServices(Module.class)
@Information(id = "shiro-hook")
public class ShiroHook implements Module, LoadCompleted {

    @RaspResource
    private RaspLog log;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private String metaInfo;

    private volatile Boolean disable = false;

    private final static String SHIRO_REMEMBER_ME = "shiro-remember-me";

    private volatile Integer shiroRememberMeAction = 0;

    private Set<String> shiroBlackKeySet = new HashSet<String>(Arrays.asList(
            // 在 1.2.4 版本前,是默认ASE秘钥,Key: kPH+bIxk5D2deZiIxcaaaA== 可以直接反序列化执行恶意代码
            "kPH+bIxk5D2deZiIxcaaaA=="
    ));


    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        this.shiroBlackKeySet = ParamSupported.getParameter(configMaps, "shiro_black_key_list", Set.class, shiroBlackKeySet);
        return true;
    }

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("org/apache/shiro/mgt/AbstractRememberMeManager")
                        .onMethod("getDecryptionCipherKey()[B", new GetDecryptionCipherKeyListener()))
                .build();
    }

    public class GetDecryptionCipherKeyListener extends AdviceListener {
        @Override
        protected void afterReturning(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            String key = Base64.encode((byte[]) advice.getReturnObj());
            for (String item : shiroBlackKeySet) {
                if (StringUtils.isNotBlank(item) && item.equals(key)) {
                    boolean enableBlock = shiroRememberMeAction == 1;
                    AttackInfo attackInfo = new AttackInfo(
                            context.get(), metaInfo, key, enableBlock,
                            "Shiro default passwd", SHIRO_REMEMBER_ME,
                            "detect shiro default cipher key: " + key,
                            100);
                    log.attack(attackInfo);
                    if (enableBlock) {
                        ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("detect shiro default cipher key block by JRASP."));
                    }
                    return;
                }
            }
        }
    }


}
