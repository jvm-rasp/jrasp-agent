package com.jrasp.agent.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 *
 * @author luanjia@taobao.com
 */
public class RaspReflectUtils {

    /**
     * 获取Java类的方法
     * 该方法不会抛出任何声明式异常
     *
     * @param clazz               类
     * @param name                方法名
     * @param parameterClassArray 参数类型数组
     * @return Java方法
     */
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

    public static Field[] getFieldsWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        final List<Field> annotatedFieldsList = getFieldsListWithAnnotation(cls, annotationCls);
        return annotatedFieldsList.toArray(new Field[annotatedFieldsList.size()]);
    }

    public static void writeField(final Field field, final Object target, final Object value, final boolean forceAccess)
            throws IllegalAccessException {
        if (forceAccess && !field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(target, value);
    }

    private static List<Field> getFieldsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        final List<Field> allFields = getAllFieldsList(cls);
        final List<Field> annotatedFields = new ArrayList<Field>();
        for (final Field field : allFields) {
            if (field.getAnnotation(annotationCls) != null) {
                annotatedFields.add(field);
            }
        }
        return annotatedFields;
    }

    private static List<Field> getAllFieldsList(final Class<?> cls) {
        final List<Field> allFields = new ArrayList<Field>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            for (final Field field : declaredFields) {
                allFields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

}
