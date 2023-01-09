package com.jrasp.agent.core.enhance.weaver;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.event.BeforeEvent;
import com.jrasp.agent.api.event.Event;
import com.jrasp.agent.api.event.InvokeEvent;
import com.jrasp.agent.api.listener.EventListener;
import com.jrasp.agent.core.classloader.BusinessClassLoaderHolder;
import com.jrasp.agent.core.util.ObjectIDs;
import com.jrasp.agent.core.util.SandboxProtector;

import java.com.jrasp.agent.bridge110.Spy;
import java.com.jrasp.agent.bridge110.SpyHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.com.jrasp.agent.bridge110.Spy.Ret.newInstanceForNone;
import static java.com.jrasp.agent.bridge110.Spy.Ret.newInstanceForThrows;

/**
 * 事件处理
 *
 * @author luanjia@taobao.com
 */
public class EventListenerHandler implements SpyHandler {

    private final static Logger logger = Logger.getLogger(EventListenerHandler.class.getName());

    private final static EventListenerHandler singleton = new EventListenerHandler();

    public static EventListenerHandler getSingleton() {
        return singleton;
    }

    // 调用序列生成器
    // private final Sequencer invokeIdSequencer = new Sequencer();
    private final AtomicInteger invokeIdSequencer = new AtomicInteger(1000);

    // 全局处理器ID:处理器映射集合
    private final Map<Integer/*LISTENER_ID*/, EventProcessor> mappingOfEventProcessor = new ConcurrentHashMap<Integer, EventProcessor>();

    /**
     * 注册事件处理器
     *
     * @param listenerId 事件监听器ID
     * @param listener   事件监听器
     */
    public void active(final int listenerId, final EventListener listener) {
        mappingOfEventProcessor.put(listenerId, new EventProcessor(listenerId, listener));
    }

    /**
     * 取消事件处理器
     *
     * @param listenerId 事件处理器ID
     */
    public void frozen(int listenerId) {
        final EventProcessor processor = mappingOfEventProcessor.remove(listenerId);
        if (null == processor) {
            logger.log(Level.FINE, "ignore frozen listener={0}, because not found.", listenerId);
            return;
        }
        // processor.clean();
    }

    /**
     * 调用出发事件处理&调用执行流程控制
     *
     * @param listenerId 处理器ID
     * @param processId  调用过程ID
     * @param invokeId   调用ID
     * @param event      调用事件
     * @param processor  事件处理器
     * @return 处理返回结果
     * @throws Throwable 当出现未知异常时,且事件处理器为中断流程事件时抛出
     */
    private Spy.Ret handleEvent(final int listenerId, final int processId,
                                final int invokeId, final Event event, final EventProcessor processor) throws Throwable {
        // 获取事件监听器
        final EventListener listener = processor.listener;

        // 调用事件处理
        try {
            listener.onEvent(event);
        } catch (ProcessControlException pce) {
            // 代码执行流程变更
            final EventProcessor.Process process = EventProcessor.processRef.get();
            final ProcessControlException.State state = pce.getState();
            // 如果流程控制要求忽略后续处理所有事件，则需要在此处进行标记
            if (pce.isIgnoreProcessEvent()) {
                process.markIgnoreProcess();
            }
            switch (state) {
                // 立即返回对象
                case RETURN_IMMEDIATELY: {
                    // 如果已经禁止后续返回任何事件了，则不进行后续的操作
                    if (pce.isIgnoreProcessEvent()) {
                        logger.log(Level.FINE, "on-event: event|{0}|{1}|{2}|{3}, ignore immediately-return-event, isIgnored.",
                                new Object[]{event.type, processId, invokeId, listenerId}
                        );
                    } else {
                        // 补偿立即返回事件
                        compensateProcessControlEvent(pce, processor, process, event);
                    }
                    // 如果是在BEFORE中立即返回，则后续不会再有RETURN事件产生
                    // 这里需要主动对齐堆栈
                    if (event.type == Event.Type.BEFORE) {
                        process.popInvokeId();
                    }
                    // 让流程立即返回
                    return Spy.Ret.newInstanceForReturn(pce.getRespond());
                }
                // 立即抛出异常
                case THROWS_IMMEDIATELY: {
                    final Throwable throwable = (Throwable) pce.getRespond();
                    // 如果已经禁止后续返回任何事件了，则不进行后续的操作
                    if (pce.isIgnoreProcessEvent()) {
                        logger.log(Level.FINE, "on-event: event|{0}|{1}|{2}|{3}, ignore immediately-throws-event, isIgnored.",
                                new Object[]{event.type,
                                        processId,
                                        invokeId,
                                        listenerId
                                }
                        );
                    } else {
                        // 如果是在BEFORE中立即抛出，则后续不会再有THROWS事件产生
                        // 这里需要主动对齐堆栈
                        if (event.type == Event.Type.BEFORE) {
                            process.popInvokeId();
                        }
                        // 标记本次异常由ImmediatelyException产生，让下次异常事件处理直接忽略
                        if (event.type != Event.Type.THROWS) {
                            process.markExceptionFromImmediately();
                        }
                        // 补偿立即抛出事件
                        compensateProcessControlEvent(pce, processor, process, event);
                    }
                    // 让流程立即抛出
                    return Spy.Ret.newInstanceForThrows(throwable);
                }

                // 什么都不操作，立即返回
                case NONE_IMMEDIATELY:
                default: {
                    return newInstanceForNone();
                }
            }
        } catch (Throwable throwable) {
            // BEFORE处理异常,打日志,并通知下游不需要进行处理
            // TODO 这里是模块bug 日志
            logger.log(Level.WARNING, "[BUG] module bug ",throwable);
        }
        // 默认返回不进行任何流程变更
        return newInstanceForNone();
    }

    // 补偿事件
    // 随着历史版本的演进，一些事件已经过期，但为了兼容API，需要在这里进行补偿
    private void compensateProcessControlEvent(ProcessControlException pce, EventProcessor processor, EventProcessor.Process process, Event event) {

        final InvokeEvent iEvent = (InvokeEvent) event;
        final Event compensateEvent;

        // 补偿立即返回事件
        if (pce.getState() == ProcessControlException.State.RETURN_IMMEDIATELY) {
            compensateEvent = process
                    .getEventFactory()
                    .makeImmediatelyReturnEvent(iEvent.processId, iEvent.invokeId, pce.getRespond());
        }

        // 补偿立即抛出事件
        else if (pce.getState() == ProcessControlException.State.THROWS_IMMEDIATELY) {
            compensateEvent = process
                    .getEventFactory()
                    .makeImmediatelyThrowsEvent(iEvent.processId, iEvent.invokeId, (Throwable) pce.getRespond());
        }

        // 异常情况不补偿
        else {
            return;
        }

        try {
            logger.log(Level.FINE, "compensate-event: event|{0}|{1}|{2}|{3} when ori-event:{4}",
                    new Object[]{compensateEvent.type,
                            iEvent.processId,
                            iEvent.invokeId,
                            processor.listenerId,
                            event.type}
            );
            processor.listener.onEvent(compensateEvent);
        } catch (Throwable cause) {
            logger.log(Level.WARNING, "compensate-event: event|" + compensateEvent.type + "|" + iEvent.processId + "|" + iEvent.invokeId + "|" + processor.listenerId + " when ori-event:" + event.type + " occur error.", cause);
        } finally {
            process.getEventFactory().returnEvent(compensateEvent);
        }
    }

    /*
     * 判断堆栈是否错位
     */
    private boolean checkProcessStack(final int processId,
                                      final int invokeId,
                                      final boolean isEmptyStack) {
        return (processId == invokeId && !isEmptyStack)
                || (processId != invokeId && isEmptyStack);
    }

    @Override
    public Spy.Ret handleOnBefore(int listenerId, int targetClassLoaderObjectID, Object[] argumentArray, String javaClassName, String javaMethodName, String javaMethodDesc, Object target) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.log(Level.FINE, "listener={0} is in protecting, ignore processing before-event", listenerId);
            return newInstanceForNone();
        }

        // 获取事件处理器
        final EventProcessor processor = mappingOfEventProcessor.get(listenerId);

        // 如果尚未注册,则直接返回,不做任何处理
        if (null == processor) {
            logger.log(Level.FINE, "listener={0} is not activated, ignore processing before-event.", listenerId);
            return newInstanceForNone();
        }

        // 获取调用跟踪信息
        final EventProcessor.Process process = EventProcessor.processRef.get();

        // 如果当前处理ID被忽略，则立即返回
        if (process.isIgnoreProcess()) {
            logger.log(Level.FINE, "listener={0} is marked ignore process!", listenerId);
            return newInstanceForNone();
        }

        // 调用ID
        final int invokeId = invokeIdSequencer.getAndIncrement();
        process.pushInvokeId(invokeId);

        // 调用过程ID
        final int processId = process.getProcessId();

        final ClassLoader javaClassLoader = ObjectIDs.instance.getObject(targetClassLoaderObjectID);
        //放置业务类加载器
        BusinessClassLoaderHolder.setBusinessClassLoader(Thread.currentThread().getContextClassLoader());
        final BeforeEvent event = process.getEventFactory().makeBeforeEvent(
                processId,
                invokeId,
                javaClassLoader,
                javaClassName,
                javaMethodName,
                javaMethodDesc,
                target,
                argumentArray
        );
        try {
            return handleEvent(listenerId, processId, invokeId, event, processor);
        } finally {
            process.getEventFactory().returnEvent(event);
        }
    }

    @Override
    public Spy.Ret handleOnThrows(int listenerId, Throwable throwable) throws Throwable {
        try {
            return handleOnEnd(listenerId, throwable, false);
        } finally {
            BusinessClassLoaderHolder.removeBusinessClassLoader();
        }
    }

    @Override
    public Spy.Ret handleOnReturn(int listenerId, Object object) throws Throwable {
        try {
            return handleOnEnd(listenerId, object, true);
        } finally {
            BusinessClassLoaderHolder.removeBusinessClassLoader();
        }
    }


    private Spy.Ret handleOnEnd(final int listenerId,
                                final Object object,
                                final boolean isReturn) throws Throwable {

        // 在守护区内产生的事件不需要响应
        if (SandboxProtector.instance.isInProtecting()) {
            logger.log(Level.FINE, "listener={0} is in protecting, ignore processing {1}-event", new Object[]{listenerId, isReturn ? "return" : "throws"});
            return newInstanceForNone();
        }

        final EventProcessor wrap = mappingOfEventProcessor.get(listenerId);

        // 如果尚未注册,则直接返回,不做任何处理
        if (null == wrap) {
            logger.log(Level.FINE, "listener={0} is not activated, ignore processing return-event|throws-event.", listenerId);
            return newInstanceForNone();
        }

        final EventProcessor.Process process = EventProcessor.processRef.get();

        // 如果当前调用过程信息堆栈是空的,说明
        // 1. BEFORE/RETURN错位
        // 2. super.<init>
        // 处理方式是直接返回,不做任何事件的处理和代码流程的改变,放弃对super.<init>的观察，可惜了
        if (process.isEmptyStack()) {

            // 修复 #194 问题
            // 需要说明的是：此修复建议由 jrasp 研发提出
            EventProcessor.processRef.remove();

            return newInstanceForNone();
        }

        // 如果异常来自于ImmediatelyException，则忽略处理直接返回抛异常
        final boolean isExceptionFromImmediately = !isReturn && process.rollingIsExceptionFromImmediately();
        if (isExceptionFromImmediately) {
            return newInstanceForThrows((Throwable) object);
        }

        // 继续异常处理
        final int processId = process.getProcessId();
        final int invokeId = process.popInvokeId();

        // 忽略事件处理
        // 放在stack.pop()后边是为了对齐执行栈
        if (process.isIgnoreProcess()) {
            return newInstanceForNone();
        }

        // 如果PID==IID说明已经到栈顶，此时需要核对堆栈是否为空
        // 如果不为空需要输出日志进行告警
        if (checkProcessStack(processId, invokeId, process.isEmptyStack())) {
            logger.log(Level.INFO, "ERROR process-stack. pid={0};iid={1};listener={2};",
                    new Object[]{processId, invokeId, listenerId}
            );
        }

        final Event event = isReturn
                ? process.getEventFactory().makeReturnEvent(processId, invokeId, object)
                : process.getEventFactory().makeThrowsEvent(processId, invokeId, (Throwable) object);

        try {
            return handleEvent(listenerId, processId, invokeId, event, wrap);
        } finally {
            process.getEventFactory().returnEvent(event);
        }

    }
}
