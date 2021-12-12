package com.jrasp.core.util;

import com.jrasp.api.listener.EventListener;
import com.jrasp.core.enhance.annotation.Interrupted;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RaspReflectUtils {

    public static Method unCaughtGetClassDeclaredJavaMethod(final Class<?> clazz,
                                                            final String name,
                                                            final Class<?>... parameterClassArray) {
        try {
            return clazz.getDeclaredMethod(name, parameterClassArray);
        } catch (NoSuchMethodException e) {
            throw new UnCaughtException(e);
        }
    }

    public static <T> T unCaughtInvokeMethod(final Method method,
                                             final Object target,
                                             final Object... parameterArray) {
        final boolean isAccessible = method.isAccessible();
        try {
            method.setAccessible(true);
            return (T) method.invoke(target, parameterArray);
        } catch (Throwable e) {
            throw new UnCaughtException(e);
        } finally {
            method.setAccessible(isAccessible);
        }
    }

    public static Field unCaughtGetClassDeclaredJavaField(final Class<?> clazz,
                                                          final String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new UnCaughtException(e);
        }
    }

    public static <T> T unCaughtGetClassDeclaredJavaFieldValue(final Class<?> clazz,
                                                               final String name,
                                                               final Object target) {
        final Field field = unCaughtGetClassDeclaredJavaField(clazz, name);
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            throw new UnCaughtException(e);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    public static void unCaughtSetClassDeclaredJavaFieldValue(final Class<?> clazz,
                                                              final String name,
                                                              final Object target,
                                                              final Object value) {
        final Field field = unCaughtGetClassDeclaredJavaField(clazz, name);
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new UnCaughtException(e);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    public static Class<?> defineClass(final ClassLoader loader,
                                       final String javaClassName,
                                       final byte[] classByteArray) throws InvocationTargetException, IllegalAccessException {

        final Method defineClassMethod =
                unCaughtGetClassDeclaredJavaMethod(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class);

        synchronized (defineClassMethod) {
            final boolean acc = defineClassMethod.isAccessible();
            try {
                defineClassMethod.setAccessible(true);
                return (Class<?>) defineClassMethod.invoke(
                        loader,
                        javaClassName,
                        classByteArray,
                        0,
                        classByteArray.length
                );
            } finally {
                defineClassMethod.setAccessible(acc);
            }
        }

    }

    /**
     * 判断是否是中断式事件处理器
     *
     * @param listenerClass 事件监听器类型
     * @return TRUE:中断式;FALSE:非中断式
     */
    public static boolean isInterruptEventHandler(final Class<? extends EventListener> listenerClass) {
        return listenerClass.isAnnotationPresent(Interrupted.class);
    }

}
