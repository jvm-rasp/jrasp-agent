package java.com.jrasp.agent.bridge110;

/**
 * 间谍处理器
 *
 * @since {@code sandbox-spy:1.3.0}
 */
public interface SpyHandler {

    /**
     * 处理方法调用:调用之前
     * <p>BEFORE</p>
     *
     * @param listenerId                事件监听器ID
     * @param targetClassLoaderObjectID 类所在ClassLoader
     * @param argumentArray             参数数组
     * @param javaClassName             类名
     * @param javaMethodName            方法名
     * @param javaMethodDesc            方法签名
     * @param target                    目标对象实例
     * @return Spy流程控制结果
     * @throws Throwable 处理{方法调用:调用之前}失败
     */
    Spy.Ret handleOnBefore(int listenerId, int targetClassLoaderObjectID, Object[] argumentArray, String javaClassName, String javaMethodName, String javaMethodDesc, Object target) throws Throwable;

    /**
     * 处理方法调用:异常返回
     *
     * @param listenerId 事件监听器ID
     * @param throwable  异常返回的异常实例
     * @return Spy流程控制结果
     * @throws Throwable 处理{方法调用:异常返回}失败
     */
    Spy.Ret handleOnThrows(int listenerId, Throwable throwable) throws Throwable;

    /**
     * 处理方法调用:正常返回
     *
     * @param listenerId 事件监听器ID
     * @param object     正常返回的对象实例
     * @return Spy流程控制结果
     * @throws Throwable 处理{方法调用:正常返回}失败
     */
    Spy.Ret handleOnReturn(int listenerId, Object object) throws Throwable;

}
