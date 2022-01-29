package com.jrasp.core.enhance.weaver;

import com.jrasp.api.event.Event;
import com.jrasp.api.listener.EventListener;
import com.jrasp.api.listener.ext.AdviceAdapterListener;
import com.jrasp.core.enhance.annotation.Interrupted;
import com.jrasp.core.util.RaspReflectUtils;
import com.jrasp.core.util.collection.GaStack;
import com.jrasp.core.util.collection.ThreadUnsafeGaStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.jrasp.core.util.RaspReflectUtils.isInterruptEventHandler;

/**
 * 事件处理器
 */
public class EventProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 处理单元
     */
    class Process {

        // 事件工厂
        private final SingleEventFactory eventFactory
                = new SingleEventFactory();

        // 调用堆栈
        private final GaStack<Integer> stack
                = new ThreadUnsafeGaStack<Integer>();

        // 是否需要忽略整个调用过程
        private boolean isIgnoreProcess = false;

        // 是否来自ImmediatelyThrowsException所抛出的异常
        private boolean isExceptionFromImmediately = false;

        /**
         * 压入调用ID
         *
         * @param invokeId 调用ID
         */
        void pushInvokeId(int invokeId) {
            if (stack.isEmpty()) {
                //第一次进入
                EventProcessor.this.setcurrentThread();
            }

            stack.push(invokeId);

            if (logger.isDebugEnabled()) {
                logger.debug("push process-stack, process-id={};invoke-id={};deep={};listener={};",
                        stack.peekLast(),
                        invokeId,
                        stack.deep(),
                        listenerId
                );
            }
        }

        /**
         * 弹出调用ID
         *
         * @return 调用ID
         */
        int popInvokeId() {
            final int invokeId;
            if (logger.isDebugEnabled()) {
                final int processId = stack.peekLast();
                invokeId = stack.pop();
                logger.debug("pop process-stack, process-id={};invoke-id={};deep={};listener={};",
                        processId,
                        invokeId,
                        stack.deep(),
                        listenerId
                );
            } else {
                invokeId = stack.pop();
            }
            return invokeId;
        }

        /**
         * 获取调用ID
         *
         * @return 调用ID
         */
        int getInvokeId() {
            return stack.peek();
        }

        /**
         * 获取调用过程ID
         *
         * @return 调用过程ID
         */
        int getProcessId() {
            return stack.peekLast();
        }

        /**
         * 是否空堆栈
         *
         * @return TRUE:是；FALSE：否
         */
        boolean isEmptyStack() {
            return stack.isEmpty();
        }

        /**
         * 当前调用过程是否需要被忽略
         *
         * @return TRUE：需要忽略；FALSE：不需要忽略
         */
        boolean isIgnoreProcess() {
            return isIgnoreProcess;
        }

        /**
         * 标记调用过程需要被忽略
         */
        void markIgnoreProcess() {
            isIgnoreProcess = true;
        }

        /**
         * 判断当前异常是否来自于ImmediatelyThrowsException，
         * 如果当前的异常来自于ImmediatelyThrowsException，则会清空当前标志位
         *
         * @return TRUE:来自于；FALSE：不来自于
         */
        boolean rollingIsExceptionFromImmediately() {
            if (isExceptionFromImmediately) {
                isExceptionFromImmediately = false;
                return true;
            }
            return false;
        }

        /**
         * 标记当前调用异常来自于ImmediatelyThrowsException
         */
        void markExceptionFromImmediately() {
            isExceptionFromImmediately = true;
        }

        /**
         * 获取事件工厂
         *
         * @return 事件工厂
         */
        SingleEventFactory getEventFactory() {
            return eventFactory;
        }

    }

    @Interrupted
    private static class InterruptedEventListenerImpl implements EventListener {

        private final EventListener listener;

        private InterruptedEventListenerImpl(EventListener listener) {
            this.listener = listener;
        }

        @Override
        public void onEvent(Event event) throws Throwable {
            listener.onEvent(event);
        }

    }

    final int listenerId;
    final EventListener listener;
    final Event.Type[] eventTypes;

    int status;
    Map<Thread,Thread> currentThreadSet;

    public final ThreadLocal<Process> processRef = new ThreadLocal<Process>() {
        @Override
        protected Process initialValue() {
            return new Process();
        }
    };

    EventProcessor(final int listenerId,
                   final EventListener listener,
                   final Event.Type[] eventTypes) {

        this.listenerId = listenerId;
        this.eventTypes = eventTypes;
        this.listener = isInterruptEventHandler(listener.getClass())
                ? new InterruptedEventListenerImpl(listener)
                : listener;
        this.status = 1;
        this.currentThreadSet = new ConcurrentHashMap(32);
    }

    /**
     *  将当前线程加入集合
     */
    public void setcurrentThread(){
        Thread t = Thread.currentThread();
        this.currentThreadSet.put(t,t);
    }

    /**
     *  冻结process
     */
    public void frozen(){
        this.status = 0;
    }

    /**
     *  激活process
     */
    public void active(){
        this.status = 1;
    }

    /**
     *  当前processor是否被冻结
     */
    public boolean isFrozen(){
        return this.status == 0;
    }

    /**
     *  当前processor是否被激活
     */
    public boolean isActivated(){
        return this.status == 1;
    }

    /**
     *  方法结束时，清理
     */
    public void clean() {
        this.currentThreadSet.remove(Thread.currentThread());
        processRef.remove();
    }

    /**
     *  清理线程集合中的线程本地变量
     */
    public void cleanThreadLocal() {
        if(null != this.currentThreadSet){
            cleanThreadLocal(this.currentThreadSet);
        }
    }

    private void cleanThreadLocal(Map<Thread,Thread> threadSet) {
        try {
            Set<Thread> threads = threadSet.keySet();
            for(Thread thread : threads){
                //反射调用 ThreadLocal.ThreadLocalMap.remove(ThreadLocal)
                Object o = RaspReflectUtils.unCaughtGetClassDeclaredJavaFieldValue(Thread.class,"threadLocals",thread);
                if(null != o){
                    Method method = RaspReflectUtils.unCaughtGetClassDeclaredJavaMethod(o.getClass(),"remove",ThreadLocal.class);
                    RaspReflectUtils.unCaughtInvokeMethod(method,o,this.processRef);
                    //AdviceAdapterListener中的opStackRef也需要释放
                    if(this.listener instanceof AdviceAdapterListener){
                        Object opStackRef = RaspReflectUtils.unCaughtGetClassDeclaredJavaFieldValue(AdviceAdapterListener.class,"opStackRef",this.listener);
                        RaspReflectUtils.unCaughtInvokeMethod(method,o,opStackRef);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("remove threadLocal error !",e);
        }
    }
}
