package com.jrasp.agent.api;

import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.request.HttpServletResponse;

import static com.jrasp.agent.api.ProcessControlException.State.*;
import static com.jrasp.agent.api.ProcessControlException.throwReturnImmediately;
import static com.jrasp.agent.api.ProcessControlException.throwThrowsImmediately;

/**
 * 流程控制
 * <p>
 * 用于控制事件处理器处理事件走向
 * </p>
 * <p>
 * 之前写的{@link ProcessControlException}进行流程控制，但命名不太规范，所以这里重命名一个类
 * </p>
 *
 * @author luanjia@taobao.com
 * @since {@code sandbox-api:1.0.10}
 */
public final class ProcessController {

    /**
     * 中断当前代码处理流程,并立即返回指定对象
     *
     * @param object 返回对象
     * @throws ProcessControlException 抛出立即返回流程控制异常
     */
    public static void returnImmediately(final Object object) throws ProcessControlException {
        throwReturnImmediately(object);
    }

    /**
     * 中断当前代码处理流程,并抛出指定异常
     *
     * @param throwable 指定异常
     * @throws ProcessControlException 抛出立即抛出异常流程控制异常
     */
    public static void throwsImmediately(final Throwable throwable) throws ProcessControlException {
        throwThrowsImmediately(throwable);
    }

    /**
     * 修改response
     * @param context
     * @param config
     * @param throwable
     * @throws ProcessControlException
     */
    public static void throwsImmediatelyAndSendResponse(Context context, RaspConfig config, Throwable throwable) throws ProcessControlException {
        try {
            HttpServletResponse response = new HttpServletResponse(context.getResponse());
            response.sendError(context, config);
        } catch (Exception e) {
            // todo 异常处理
            // 先 ignore
        }
        throw new ProcessControlException(ProcessControlException.State.THROWS_IMMEDIATELY, throwable);
    }

    /**
     * 修改response
     * @param attackInfo
     * @param config
     * @param throwable
     * @throws ProcessControlException
     */
    public static void throwsImmediatelyAndSendResponse(AttackInfo attackInfo, RaspConfig config, Throwable throwable) throws ProcessControlException {
        try {
            Context context = attackInfo.getContext();
            HttpServletResponse response = new HttpServletResponse(context.getResponse());
            response.sendError(attackInfo, config);
        } catch (Exception e) {
            // todo 异常处理
            // 先 ignore
            // 仅jsp、@Controller 才能修改返回的页面
            // Caused by: java.lang.IllegalStateException: getWriter() has already been called for this response
        }
        throw new ProcessControlException(ProcessControlException.State.THROWS_IMMEDIATELY, throwable);
    }

    /**
     * 中断当前代码处理流程,并立即返回指定对象,且忽略后续所有事件处理
     *
     * @param object 返回对象
     * @throws ProcessControlException 抛出立即返回流程控制异常
     * @since {@code sandbox-api:1.0.16}
     */
    public static void returnImmediatelyWithIgnoreProcessEvent(final Object object) throws ProcessControlException {
        throw new ProcessControlException(true, RETURN_IMMEDIATELY, object);
    }

    /**
     * 中断当前代码处理流程,并抛出指定异常,且忽略后续所有事件处理
     *
     * @param throwable 指定异常
     * @throws ProcessControlException 抛出立即抛出异常流程控制异常
     * @since {@code sandbox-api:1.0.16}
     */
    public static void throwsImmediatelyWithIgnoreProcessEvent(final Throwable throwable) throws ProcessControlException {
        throw new ProcessControlException(true, THROWS_IMMEDIATELY, throwable);
    }

    private static final ProcessControlException noneImmediatelyException
            = new ProcessControlException(NONE_IMMEDIATELY, null);

    private static final ProcessControlException noneImmediatelyWithIgnoreProcessEventException
            = new ProcessControlException(true, NONE_IMMEDIATELY, null);

    /**
     * 不干预当前处理流程
     *
     * @throws ProcessControlException 抛出不干预流程处理异常
     * @since {@code sandbox-api:1.0.16}
     */
    public static void noneImmediately() throws ProcessControlException {
        throw noneImmediatelyException;
    }

    /**
     * 不干预当前处理流程,但忽略后续所有事件处理
     *
     * @throws ProcessControlException 抛出不干预流程处理异常
     * @since {@code sandbox-api:1.0.16}
     */
    public static void noneImmediatelyWithIgnoreProcessEvent() throws ProcessControlException {
        throw noneImmediatelyWithIgnoreProcessEventException;
    }

}
