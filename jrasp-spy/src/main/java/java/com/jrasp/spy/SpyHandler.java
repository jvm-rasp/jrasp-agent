package java.com.jrasp.spy;

public interface SpyHandler {
    void handleOnCallBefore(int listenerId, int lineNumber, String owner, String name, String desc) throws Throwable;

    void handleOnCallReturn(int listenerId) throws Throwable;

    void handleOnCallThrows(int listenerId, String throwException) throws Throwable;

    void handleOnLine(int listenerId, int lineNumber) throws Throwable;

    Spy.Ret handleOnBefore(int listenerId, int targetClassLoaderObjectID, Object[] argumentArray, String javaClassName, String javaMethodName, String javaMethodDesc, Object target) throws Throwable;

    Spy.Ret handleOnThrows(int listenerId, Throwable throwable) throws Throwable;

    Spy.Ret handleOnReturn(int listenerId, Object object) throws Throwable;
}