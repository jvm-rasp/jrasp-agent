package com.jrasp.core.manager.impl;

import com.jrasp.api.event.Event;
import com.jrasp.api.filter.Filter;
import com.jrasp.api.listener.EventListener;
import com.jrasp.api.listener.ext.EventWatchCondition;
import com.jrasp.api.log.Log;
import com.jrasp.api.resource.ModuleEventWatcher;
import com.jrasp.core.CoreModule;
import com.jrasp.core.enhance.weaver.EventListenerHandler;
import com.jrasp.core.log.LogFactory;
import com.jrasp.core.manager.CoreLoadedClassDataSource;
import com.jrasp.core.util.Sequencer;
import com.jrasp.core.util.matcher.ExtFilterMatcher;
import com.jrasp.core.util.matcher.GroupMatcher;
import com.jrasp.core.util.matcher.Matcher;
import org.apache.commons.collections.CollectionUtils;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.jrasp.api.filter.ExtFilter.ExtFilterFactory.make;
import static com.jrasp.core.util.matcher.ExtFilterMatcher.toOrGroupMatcher;

/**
 * 默认事件观察者实现
 */
public class DefaultModuleEventWatcher implements ModuleEventWatcher {

    private final Log logger = LogFactory.getLog(getClass());

    private final Instrumentation inst;
    private final CoreLoadedClassDataSource classDataSource;
    private final CoreModule coreModule;
    private final boolean isEnableUnsafe;
    private final String namespace;

    // 观察ID序列生成器
    private final Sequencer watchIdSequencer = new Sequencer();

    DefaultModuleEventWatcher(final Instrumentation inst,
                              final CoreLoadedClassDataSource classDataSource,
                              final CoreModule coreModule,
                              final boolean isEnableUnsafe,
                              final String namespace) {
        this.inst = inst;
        this.classDataSource = classDataSource;
        this.coreModule = coreModule;
        this.isEnableUnsafe = isEnableUnsafe;
        this.namespace = namespace;
    }


    // 开始进度
    private void beginProgress(final Progress progress,
                               final int total) {
        if (null != progress) {
            try {
                progress.begin(total);
            } catch (Throwable cause) {
                logger.warn("begin progress failed.", cause);
            }
        }
    }

    // 结束进度
    private void finishProgress(final Progress progress, final int cCnt, final int mCnt) {
        if (null != progress) {
            try {
                progress.finish(cCnt, mCnt);
            } catch (Throwable cause) {
                logger.warn("finish progress failed.", cause);
            }
        }
    }

    /*
     * 形变观察所影响的类
     */
    private void reTransformClasses(
        final int watchId,
        final List<Class<?>> waitingReTransformClasses,
        final Progress progress) {
        // 需要形变总数
        final int total = waitingReTransformClasses.size();

        // 如果找不到需要被重新增强的类则直接返回
        if (CollectionUtils.isEmpty(waitingReTransformClasses)) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("reTransformClasses={};module={};watch={};",
                    waitingReTransformClasses, coreModule.getUniqueId(), watchId);
        }

        int index = 0;
        for (final Class<?> waitingReTransformClass : waitingReTransformClasses) {
            index++;
            try {
                if (null != progress) {
                    try {
                        progress.progressOnSuccess(waitingReTransformClass, index);
                    } catch (Throwable cause) {
                        // 在进行进度汇报的过程中抛出异常,直接进行忽略,因为不影响形变的主体流程
                        // 仅仅只是一个汇报作用而已
                        logger.warn("watch={} in module={} on {} report progressOnSuccess occur exception at index={};total={};",
                                watchId, coreModule.getUniqueId(), waitingReTransformClass,
                                index - 1, total,
                                cause
                        );
                    }
                }
                inst.retransformClasses(waitingReTransformClass);
                logger.info("watch={} in module={} single reTransform {} success, at index={};total={};",
                        watchId, coreModule.getUniqueId(), waitingReTransformClass,
                        index - 1, total
                );
            } catch (Throwable causeOfReTransform) {
                logger.warn("watch={} in module={} single reTransform {} failed, at index={};total={}. ignore this class.",
                        watchId, coreModule.getUniqueId(), waitingReTransformClass,
                        index - 1, total,
                        causeOfReTransform
                );
                if (null != progress) {
                    try {
                        progress.progressOnFailed(waitingReTransformClass, index, causeOfReTransform);
                    } catch (Throwable cause) {
                        logger.warn("watch={} in module={} on {} report progressOnFailed occur exception, at index={};total={};",
                                watchId, coreModule.getUniqueId(), waitingReTransformClass,
                                index - 1, total,
                                cause
                        );
                    }
                }
            }
        }//for

    }

    @Override
    public int watch(final Filter filter,
                     final EventListener listener,
                     final Event.Type... eventType) {
        return watch(filter, listener, null, eventType);
    }

    @Override
    public int watch(final Filter filter,
                     final EventListener listener,
                     final Progress progress,
                     final Event.Type... eventType) {
        return watch(new ExtFilterMatcher(make(filter)), listener, progress, eventType);
    }

    @Override
    public int watch(final EventWatchCondition condition,
                     final EventListener listener,
                     final Progress progress,
                     final Event.Type... eventType) {
        return watch(toOrGroupMatcher(condition.getOrFilterArray()), listener, progress, eventType);
    }

    // 这里是用matcher重制过后的watch
    private int watch(final Matcher matcher,
                      final EventListener listener,
                      final Progress progress,
                      final Event.Type... eventType) {
        final int watchId = watchIdSequencer.next();
        // 给对应的模块追加ClassFileTransformer
        final RaspClassFileTransformer raspClassFileTransformer = new RaspClassFileTransformer(inst,
                watchId, coreModule.getUniqueId(), matcher, listener, isEnableUnsafe, eventType, namespace);

        // 注册到CoreModule中
        coreModule.getRaspClassFileTransformers().add(raspClassFileTransformer);

        //这里addTransformer后，接下来引起的类加载都会经过raspClassFileTransformer
        inst.addTransformer(raspClassFileTransformer, true);

        // 查找需要渲染的类集合
        final List<Class<?>> waitingReTransformClasses = classDataSource.findForReTransform(matcher);
        logger.info("watch={} in module={} found {} classes for watch(ing).",
                watchId,
                coreModule.getUniqueId(),
                waitingReTransformClasses.size()
        );

        int cCnt = 0, mCnt = 0;

        // 进度通知启动
        beginProgress(progress, waitingReTransformClasses.size());
        try {

            // 应用JVM
            reTransformClasses(watchId,waitingReTransformClasses, progress);

            // 计数
            cCnt += raspClassFileTransformer.getAffectStatistic().cCnt();
            mCnt += raspClassFileTransformer.getAffectStatistic().mCnt();


            // 激活增强类
            if (coreModule.isActivated()) {
                final int listenerId = raspClassFileTransformer.getListenerId();
                EventListenerHandler.getSingleton().active(listenerId, listener, eventType);
            }

        } finally {
            finishProgress(progress, cCnt, mCnt);
        }

        return watchId;
    }

    @Override
    public void delete(final int watcherId,
                       final Progress progress) {

        final Set<Matcher> waitingRemoveMatcherSet = new LinkedHashSet<Matcher>();

        // 找出待删除的RaspClassFileTransformer
        final Iterator<RaspClassFileTransformer> cftIt = coreModule.getRaspClassFileTransformers().iterator();
        int cCnt = 0, mCnt = 0;
        while (cftIt.hasNext()) {
            final RaspClassFileTransformer raspClassFileTransformer = cftIt.next();
            if (watcherId == raspClassFileTransformer.getWatchId()) {

                // 冻结所有关联代码增强
                EventListenerHandler.getSingleton()
                        .frozen(raspClassFileTransformer.getListenerId());

                // 在JVM中移除掉命中的ClassFileTransformer
                inst.removeTransformer(raspClassFileTransformer);

                // 计数
                cCnt += raspClassFileTransformer.getAffectStatistic().cCnt();
                mCnt += raspClassFileTransformer.getAffectStatistic().mCnt();

                // 追加到待删除过滤器集合
                waitingRemoveMatcherSet.add(raspClassFileTransformer.getMatcher());

                // 清除掉该RaspClassFileTransformer
                cftIt.remove();

            }
        }

        // 查找需要删除后重新渲染的类集合
        final List<Class<?>> waitingReTransformClasses = classDataSource.findForReTransform(
                new GroupMatcher.Or(waitingRemoveMatcherSet.toArray(new Matcher[0]))
        );
        logger.info("watch={} in module={} found {} classes for delete.",
                watcherId,
                coreModule.getUniqueId(),
                waitingReTransformClasses.size()
        );

        beginProgress(progress, waitingReTransformClasses.size());
        try {
            // 应用JVM
            reTransformClasses(watcherId, waitingReTransformClasses, progress);
        } finally {
            finishProgress(progress, cCnt, mCnt);
        }
    }

    @Override
    public void delete(int watcherId) {
        delete(watcherId, null);
    }

    @Override
    public void watching(Filter filter, EventListener listener, WatchCallback watchCb, Event.Type... eventType) throws Throwable {
        watching(filter, listener, null, watchCb, null, eventType);
    }

    @Override
    public void watching(final Filter filter,
                         final EventListener listener,
                         final Progress wProgress,
                         final WatchCallback watchCb,
                         final Progress dProgress,
                         final Event.Type... eventType) throws Throwable {
        final int watchId = watch(new ExtFilterMatcher(make(filter)), listener, wProgress, eventType);
        try {
            watchCb.watchCompleted();
        } finally {
            delete(watchId, dProgress);
        }
    }

}
