package com.jrasp.agent.core.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sandbox守护者
 * <p>
 * 用来保护sandbox的操作所产生的事件不被响应
 * </p>
 *
 * @author oldmanpushcart@gamil.com
 */
public class SandboxProtector {

    private final static Logger logger = Logger.getLogger(SandboxProtector.class.getName());

    public static final SandboxProtector instance = new SandboxProtector();

    // 静态变量声明为 static
    // TODO 卸载时显示清除
    public final static ThreadLocal<AtomicInteger> isInProtectingThreadLocal = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    /**
     * 进入守护区域
     *
     * @return 守护区域当前引用计数
     */
    public int enterProtecting() {
        return isInProtectingThreadLocal.get().getAndIncrement();
    }

    /**
     * 离开守护区域
     *
     * @return 守护区域当前引用计数
     */
    public int exitProtecting() {
        final int referenceCount = isInProtectingThreadLocal.get().decrementAndGet();
        if (referenceCount == 0) {
            isInProtectingThreadLocal.remove();
        } else if (referenceCount < 0) {
            logger.log(Level.WARNING, "thread:{0} exit protect:{1} with error!", new Object[]{Thread.currentThread(), referenceCount});
        }
        return referenceCount;
    }

    /**
     * 判断当前是否处于守护区域中
     *
     * @return TRUE:在守护区域中；FALSE：非守护区域中
     */
    public boolean isInProtecting() {
        boolean res = isInProtectingThreadLocal.get().get() > 0;
        if (!res) {
            // 为0必须清除
            isInProtectingThreadLocal.remove();
        }
        return res;
    }

    /**
     * 守护接口定义的所有方法
     *
     * @param protectTargetInterface 保护目标接口类型
     * @param protectTarget          保护目标接口实现
     * @param <T>                    接口类型
     * @return 被保护的目标接口实现
     */
    @SuppressWarnings("unchecked")
    public <T> T protectProxy(final Class<T> protectTargetInterface,
                              final T protectTarget) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{protectTargetInterface}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                final int enterReferenceCount = enterProtecting();
                try {
                    return method.invoke(protectTarget, args);
                } finally {
                    final int exitReferenceCount = exitProtecting();
                    // assert enterReferenceCount == exitReferenceCount;
                    if (enterReferenceCount != exitReferenceCount) {
                        logger.log(Level.WARNING, "thread:{0} exit protecting with error!, expect:{1} actual:{2}",
                                new Object[]{
                                        Thread.currentThread(),
                                        enterReferenceCount,
                                        exitReferenceCount
                                }
                        );
                    }
                }
            }

        });
    }
}
