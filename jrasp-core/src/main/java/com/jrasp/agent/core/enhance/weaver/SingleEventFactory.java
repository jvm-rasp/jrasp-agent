package com.jrasp.agent.core.enhance.weaver;

import com.jrasp.agent.api.event.*;
import com.jrasp.agent.core.util.UnCaughtException;
import com.jrasp.agent.core.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static com.jrasp.agent.core.util.RaspReflectUtils.unCaughtGetClassDeclaredJavaField;

/**
 * 单例事件工厂
 */
class SingleEventFactory {

    private static final int ILLEGAL_PROCESS_ID = -1;
    private static final int ILLEGAL_INVOKE_ID = -1;

    private static final Unsafe unsafe;
    private static final long processIdFieldInInvokeEventOffset;
    private static final long invokeIdFieldInInvokeEventOffset;
    private static final long javaClassLoaderFieldInBeforeEventOffset;
    private static final long javaClassNameFieldInBeforeEventOffset;
    private static final long javaMethodNameFieldInBeforeEventOffset;
    private static final long javaMethodDescFieldInBeforeEventOffset;
    private static final long targetFieldInBeforeEventOffset;
    private static final long argumentArrayFieldInBeforeEventOffset;
    private static final long objectFieldInReturnEventOffset;
    private static final long throwableFieldInThrowsEventOffset;

    static {
        try {
            unsafe = UnsafeUtils.getUnsafe();
            processIdFieldInInvokeEventOffset = unsafe.objectFieldOffset(InvokeEvent.class.getDeclaredField("processId"));
            invokeIdFieldInInvokeEventOffset = unsafe.objectFieldOffset(InvokeEvent.class.getDeclaredField("invokeId"));
            javaClassLoaderFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaClassLoader"));
            javaClassNameFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaClassName"));
            javaMethodNameFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaMethodName"));
            javaMethodDescFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("javaMethodDesc"));
            targetFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("target"));
            argumentArrayFieldInBeforeEventOffset = unsafe.objectFieldOffset(BeforeEvent.class.getDeclaredField("argumentArray"));
            objectFieldInReturnEventOffset = unsafe.objectFieldOffset(ReturnEvent.class.getDeclaredField("object"));
            throwableFieldInThrowsEventOffset = unsafe.objectFieldOffset(ThrowsEvent.class.getDeclaredField("throwable"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private BeforeEvent beforeEvent = null;
    private ReturnEvent returnEvent = null;
    private ThrowsEvent throwsEvent = null;
    private ImmediatelyThrowsEvent immediatelyThrowsEvent = null;
    private ImmediatelyReturnEvent immediatelyReturnEvent = null;


    public BeforeEvent makeBeforeEvent(final int processId,
                                       final int invokeId,
                                       final ClassLoader javaClassLoader,
                                       final String javaClassName,
                                       final String javaMethodName,
                                       final String javaMethodDesc,
                                       final Object target,
                                       final Object[] argumentArray) {
        if (null == beforeEvent) {
            beforeEvent = new BeforeEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null, null, null, null, null, null);
        }
        unsafe.putInt(beforeEvent, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(beforeEvent, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(beforeEvent, javaClassLoaderFieldInBeforeEventOffset, javaClassLoader);
        unsafe.putObject(beforeEvent, javaClassNameFieldInBeforeEventOffset, javaClassName);
        unsafe.putObject(beforeEvent, javaMethodNameFieldInBeforeEventOffset, javaMethodName);
        unsafe.putObject(beforeEvent, javaMethodDescFieldInBeforeEventOffset, javaMethodDesc);
        unsafe.putObject(beforeEvent, targetFieldInBeforeEventOffset, target);
        unsafe.putObject(beforeEvent, argumentArrayFieldInBeforeEventOffset, argumentArray);
        return beforeEvent;
    }

    public ReturnEvent makeReturnEvent(final int processId,
                                       final int invokeId,
                                       final Object returnObj) {
        if (null == returnEvent) {
            returnEvent = new ReturnEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
        }
        unsafe.putInt(returnEvent, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(returnEvent, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(returnEvent, objectFieldInReturnEventOffset, returnObj);
        return returnEvent;
    }

    public ImmediatelyReturnEvent makeImmediatelyReturnEvent(final int processId,
                                                             final int invokeId,
                                                             final Object returnObj) {
        if (null == immediatelyReturnEvent) {
            immediatelyReturnEvent = new ImmediatelyReturnEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
        }
        unsafe.putInt(immediatelyReturnEvent, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(immediatelyReturnEvent, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(immediatelyReturnEvent, objectFieldInReturnEventOffset, returnObj);
        return immediatelyReturnEvent;
    }

    public ThrowsEvent makeThrowsEvent(final int processId,
                                       final int invokeId,
                                       final Throwable throwable) {
        if (null == throwsEvent) {
            throwsEvent = new ThrowsEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
        }
        unsafe.putInt(throwsEvent, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(throwsEvent, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(throwsEvent, throwableFieldInThrowsEventOffset, throwable);
        return throwsEvent;
    }

    public ImmediatelyThrowsEvent makeImmediatelyThrowsEvent(final int processId,
                                                             final int invokeId,
                                                             final Throwable throwable) {
        if (null == immediatelyThrowsEvent) {
            immediatelyThrowsEvent = new ImmediatelyThrowsEvent(ILLEGAL_PROCESS_ID, ILLEGAL_INVOKE_ID, null);
        }
        unsafe.putInt(immediatelyThrowsEvent, processIdFieldInInvokeEventOffset, processId);
        unsafe.putInt(immediatelyThrowsEvent, invokeIdFieldInInvokeEventOffset, invokeId);
        unsafe.putObject(immediatelyThrowsEvent, throwableFieldInThrowsEventOffset, throwable);
        return immediatelyThrowsEvent;
    }

    private final static Field throwableFieldInThrowsEvent = unCaughtGetClassDeclaredJavaField(ThrowsEvent.class, "throwable");
    private final static Field objectFieldInReturnEvent = unCaughtGetClassDeclaredJavaField(ReturnEvent.class, "object");

    static {
        throwableFieldInThrowsEvent.setAccessible(true);
        objectFieldInReturnEvent.setAccessible(true);
    }

    public void returnEvent(Event event) {
        switch (event.type) {
            case BEFORE:
                unsafe.putObject(event, targetFieldInBeforeEventOffset, null);
                unsafe.putObject(event, argumentArrayFieldInBeforeEventOffset, null);
                break;
            case IMMEDIATELY_THROWS:
            case THROWS:
                // FIXED #130
                // unsafe.putObject(event, throwableFieldInThrowsEventOffset, null);
                try{
                    throwableFieldInThrowsEvent.set(event, null);
                }catch (IllegalAccessException e){
                    throw new UnCaughtException(e);
                }
                break;
            case IMMEDIATELY_RETURN:
            case RETURN:
                // FIXED #130
                // unsafe.putObject(event, objectFieldInReturnEventOffset, null);
                try {
                    objectFieldInReturnEvent.set(event, null);
                }catch (IllegalAccessException e){
                    throw new UnCaughtException(e);
                }
                break;
        }
    }

}
