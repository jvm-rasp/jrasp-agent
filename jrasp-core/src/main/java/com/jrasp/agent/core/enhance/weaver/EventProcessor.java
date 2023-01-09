package com.jrasp.agent.core.enhance.weaver;

import com.jrasp.agent.api.listener.EventListener;
import com.jrasp.agent.core.util.collection.GaStack;
import com.jrasp.agent.core.util.collection.ThreadUnsafeGaStack;

/**
 * 事件处理器
 */
public class EventProcessor {

    public final int listenerId;
    public final EventListener listener;
    public final static ThreadLocal<Process> processRef = new ThreadLocal<Process>() {
        @Override
        protected Process initialValue() {
            return new Process();
        }
    };

    EventProcessor(final int listenerId,
                   final EventListener listener) {

        this.listenerId = listenerId;
        this.listener = listener;
    }

    /**
     * 处理单元
     */
    static class Process {

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
            stack.push(invokeId);
        }

        /**
         * 弹出调用ID
         *
         * @return 调用ID
         */
        int popInvokeId() {
            final int invokeId;
            invokeId = stack.pop();
            if (stack.isEmpty()) {
                processRef.remove();
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
}
