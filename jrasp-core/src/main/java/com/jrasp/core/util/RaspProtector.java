package com.jrasp.core.util;

import com.jrasp.api.log.Log;
import com.jrasp.core.log.LogFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jrasp.core.log.AgentLogIdConstant.AGENT_COMMON_LOG_ID;

public class RaspProtector {

    private final static Log logger = LogFactory.getLog(RaspProtector.class);

    private final ThreadLocal<AtomicInteger> isInProtectingThreadLocal = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    public int enterProtecting() {
        final int referenceCount = isInProtectingThreadLocal.get().getAndIncrement();
        if (logger.isDebugEnabled()) {
            logger.debug(AGENT_COMMON_LOG_ID,"thread:{} enter protect:{}", Thread.currentThread(), referenceCount);
        }
        return referenceCount;
    }

    public int exitProtecting() {
        final int referenceCount = isInProtectingThreadLocal.get().decrementAndGet();
        assert referenceCount >= 0;
        if (referenceCount == 0) {
            isInProtectingThreadLocal.remove();
            if (logger.isDebugEnabled()) {
                logger.debug(AGENT_COMMON_LOG_ID,"thread:{} exit protect:{} with clean", Thread.currentThread(), referenceCount);
            }
        } else if (referenceCount > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug(AGENT_COMMON_LOG_ID,"thread:{} exit protect:{}", Thread.currentThread(), referenceCount);
            }
        } else {
            logger.warn(AGENT_COMMON_LOG_ID,"thread:{} exit protect:{} with error!", Thread.currentThread(), referenceCount);
        }
        return referenceCount;
    }

    public boolean isInProtecting() {
        return isInProtectingThreadLocal.get().get() > 0;
    }

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
                    assert enterReferenceCount == exitReferenceCount;
                    if (enterReferenceCount != exitReferenceCount) {
                        logger.warn(AGENT_COMMON_LOG_ID,"thread:{} exit protecting with error!, expect:{} actual:{}",
                                Thread.currentThread(),
                                enterReferenceCount,
                                exitReferenceCount
                        );
                    }
                }
            }

        });
    }

    public static final RaspProtector instance = new RaspProtector();

}
