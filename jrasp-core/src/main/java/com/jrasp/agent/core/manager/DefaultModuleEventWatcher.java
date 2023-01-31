package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.listener.AdviceAdapterListener;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.enhance.weaver.EventListenerHandler;
import com.jrasp.agent.core.util.ObjectIDs;
import com.jrasp.agent.core.util.RaspClassUtils;
import com.jrasp.agent.core.util.string.RaspStringUtils;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DefaultModuleEventWatcher implements ModuleEventWatcher {

    private final static Logger logger = Logger.getLogger(DefaultModuleEventWatcher.class.getName());

    private final Instrumentation inst;

    private final DefaultCoreLoadedClassDataSource classDataSource;

    private final CoreModule coreModule;

    DefaultModuleEventWatcher(final Instrumentation inst, final DefaultCoreLoadedClassDataSource classDataSource, final CoreModule coreModule) {
        this.inst = inst;
        this.classDataSource = classDataSource;
        this.coreModule = coreModule;
    }

    /**
     * 批量接口,效率更高
     * 模块与内核传递 hook类的接口
     *
     * @param matchers
     */
    @Override
    public void watch(final Set<ClassMatcher> matchers) {
        // 全局 ClassFileTransformer
        // 存储全部 matcher
        // TODO 首次初始化，可以将所有matchers一次加入到targetClazzMap
        // TODO 大大提高agent加载速度
        Set<String> classNameSets = new HashSet<String>();
        for (ClassMatcher classMatcher : matchers) {
            String classNameJdk = classMatcher.getClassNameJdk();
            classNameSets.add(classNameJdk);
            if (classMatcher.isForceLoad()) {
                RaspClassUtils.doEarlyLoadSandboxClass(classNameJdk);
            }
            RaspClassFileTransformer.INSTANCE.targetClazzMap.put(classMatcher.getClassName(), classMatcher);
            coreModule.getClassMatchers().add(classMatcher);
        }

        // 查找需要渲染的类集合
        final List<Class<?>> waitingReTransformClasses = classDataSource.findTargetClass(classNameSets);

        // jvm已经加载的类，需要重新转换
        reTransformClasses(waitingReTransformClasses, true);

        // 激活增强类
        if (coreModule.isActivated()) {
            for (ClassMatcher matcher : matchers) {
                Map<String, MethodMatcher> methodMatcherList = matcher.getMethodMatcherMap();
                for (Map.Entry<String, MethodMatcher> entry : methodMatcherList.entrySet()) {
                    AdviceListener adviceListener = entry.getValue().getAdviceListener();
                    int listenerId = ObjectIDs.instance.identity(adviceListener);
                    EventListenerHandler.getSingleton().active(listenerId, new AdviceAdapterListener(adviceListener));
                }
            }
        }
    }

    @Override
    public void delete(final Set<ClassMatcher> matchers) {
        Set<String> classNameJdk = new HashSet<String>();
        for (ClassMatcher classMatcher : matchers) {
            classNameJdk.add(classMatcher.getClassNameJdk());
        }
        final List<Class<?>> waitingReTransformClasses = classDataSource.findTargetClass(classNameJdk);
        // 先将 classMatcher 删除
        for (ClassMatcher classMatcher : matchers) {
            RaspClassFileTransformer.INSTANCE.targetClazzMap.remove(classMatcher.getClassName());
            coreModule.getClassMatchers().remove(classMatcher);
        }
        reTransformClasses(waitingReTransformClasses, false);
    }

    /**
     * @param waitingReTransformClasses 待转换的类
     * @param isAddHook
     */
    private void reTransformClasses(final List<Class<?>> waitingReTransformClasses, final boolean isAddHook) {
        // 如果找不到需要被重新增强的类则直接返回
        if (waitingReTransformClasses == null || waitingReTransformClasses.size() == 0) {
            return;
        }
        // 在rasp 加载前，已经被JVM加载的类，需要进行一次reTransformClasses，加入 rasp hook 逻辑
        // 在rasp 卸载时，对于全部HOOK类，需要进行一次reTransformClasses，还原
        logger.log(Level.CONFIG, "isAddHook: {0}, transform class: {1}",
                new Object[]{isAddHook, RaspStringUtils.join(waitingReTransformClasses, ",")});
        for (final Class<?> waitingReTransformClass : waitingReTransformClasses) {
            try {
                inst.retransformClasses(waitingReTransformClass);
            } catch (Throwable cause) {
                logger.log(Level.WARNING, "instruemnt retransform class: {0}, error: {1}, cause: {2}",
                        new Object[]{waitingReTransformClass.getName(), cause.getMessage(), cause.getCause()});
            }
        }
    }

}
