package com.jrasp.agent.api.listener;

import com.jrasp.agent.api.event.*;

import java.util.Stack;

/**
 * 通知监听器
 *
 * @author luanjia@taobao.com
 * @since {@code sandbox-api:1.0.10}
 */
public class AdviceAdapterListener implements EventListener {

    private final AdviceListener adviceListener;

    public AdviceAdapterListener(final AdviceListener adviceListener) {
        this.adviceListener = adviceListener;
    }

    public static final ThreadLocal<OpStack> opStackRef = new ThreadLocal<OpStack>() {
        @Override
        protected OpStack initialValue() {
            return new OpStack();
        }
    };

    @Override
    final public void onEvent(final Event event) throws Throwable {
        final OpStack opStack = opStackRef.get();
        try {
            switchEvent(opStack, event);
        } finally {
            // 如果执行到TOP的最后一个事件，则需要主动清理占用的资源
            if (opStack.isEmpty()) {
                opStackRef.remove();
            }
        }
    }

    // 执行事件
    private void switchEvent(final OpStack opStack, final Event event) throws Throwable {
        switch (event.type) {
            case BEFORE: {
                final BeforeEvent bEvent = (BeforeEvent) event;
                final ClassLoader loader = toClassLoader(bEvent.javaClassLoader);
                final Advice advice = new Advice(bEvent.processId, bEvent.invokeId, loader, bEvent.argumentArray, bEvent.target);

                final Advice top, parent;

                // 顶层调用
                if (opStack.isEmpty()) {
                    top = parent = advice;
                } else {
                    parent = opStack.peek();
                    top = parent.getProcessTop();
                }
                advice.applyBefore(top, parent);

                opStackRef.get().pushForBegin(advice);
                adviceListener.before(advice);
                break;
            }

            // 这里需要感知到IMMEDIATELY，修复#117
            case IMMEDIATELY_THROWS:
            case IMMEDIATELY_RETURN: {
                final InvokeEvent invokeEvent = (InvokeEvent) event;
                opStack.popByExpectInvokeId(invokeEvent.invokeId);
                // 修复#123
                break;
            }

            case RETURN: {
                final ReturnEvent rEvent = (ReturnEvent) event;
                final Advice wrapAdvice = opStack.popByExpectInvokeId(rEvent.invokeId);
                if (null != wrapAdvice) {
                    Advice advice = wrapAdvice.applyReturn(rEvent.object);
                    try {
                        adviceListener.afterReturning(advice);
                    } finally {
                        adviceListener.after(advice);
                    }
                }
                break;
            }
            case THROWS: {
                final ThrowsEvent tEvent = (ThrowsEvent) event;
                final Advice wrapAdvice = opStack.popByExpectInvokeId(tEvent.invokeId);
                if (null != wrapAdvice) {
                    Advice advice = wrapAdvice.applyThrows(tEvent.throwable);
                    try {
                        adviceListener.afterThrowing(advice);
                    } finally {
                        adviceListener.after(advice);
                    }
                }
                break;
            }

            default:

        }
    }

    /**
     * 通知操作堆栈
     */
    private static class OpStack {

        private final Stack<Advice> adviceStack = new Stack<Advice>();

        boolean isEmpty() {
            return adviceStack.isEmpty();
        }

        Advice peek() {
            return adviceStack.peek();
        }

        void pushForBegin(final Advice advice) {
            adviceStack.push(advice);
        }

        /**
         * 在通知堆栈中，BEFORE:[RETURN/THROWS]的invokeId是配对的，
         * 如果发生错位则说明BEFORE的事件没有被成功压入堆栈，没有被正确的处理，外界没有正确感知BEFORE
         * 所以这里也要进行修正行的忽略对应的[RETURN/THROWS]
         *
         * @param expectInvokeId 期待的invokeId
         *                       必须要求和BEFORE的invokeId配对
         * @return 如果invokeId配对成功，则返回对应的Advice，否则返回null
         */
        Advice popByExpectInvokeId(final int expectInvokeId) {
            return !adviceStack.isEmpty()
                    && adviceStack.peek().getInvokeId() == expectInvokeId
                    ? adviceStack.pop()
                    : null;
        }

        Advice peekByExpectInvokeId(final int expectInvokeId) {
            return !adviceStack.isEmpty()
                    && adviceStack.peek().getInvokeId() == expectInvokeId
                    ? adviceStack.peek()
                    : null;
        }

    }

    // 提取ClassLoader，从BeforeEvent中获取到的ClassLoader
    private ClassLoader toClassLoader(ClassLoader loader) {
        return null == loader
                // 如果此处为null，则说明遇到了来自Bootstrap的类，
                ? AdviceAdapterListener.class.getClassLoader()
                : loader;
    }

}
