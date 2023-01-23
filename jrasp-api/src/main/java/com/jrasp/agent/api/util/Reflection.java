package com.jrasp.agent.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * from open-rasp
 */
public class Reflection {

    /**
     * 根据方法名调用对象的某一个方法
     *
     * @param object     调用方法的对象
     * @param methodName 方法名称
     * @param paramTypes 参数类型列表
     * @param parameters 参数列表
     * @return 方法返回值
     */
    public static Object invokeMethod(Object object, String methodName, Class[] paramTypes, Object... parameters) throws Exception {
        if (object == null) {
            return null;
        }
        return invokeMethod(object, object.getClass(), methodName, paramTypes, parameters);
    }

    /**
     * 反射调用方法，并把返回值进行强制转换为String
     *
     * @return 被调用函数返回的String
     * @see #invokeMethod(Object, String, Class[], Object...)
     */
    public static String invokeStringMethod(Object object, String methodName, Class[] paramTypes, Object... parameters) throws Exception {
        Object ret = invokeMethod(object, methodName, paramTypes, parameters);
        return ret != null ? (String) ret : null;
    }

    /**
     * 反射调用方法，并把返回值进行强制转换为Integer
     *
     * @return 被调用函数返回的Integer
     * @see #invokeMethod(Object, String, Class[], Object...)
     */
    public static Integer invokeIntegerMethod(Object object, String methodName, Class[] paramTypes, Object... parameters) throws Exception {
        Object ret = invokeMethod(object, methodName, paramTypes, parameters);

        return Integer.parseInt(ret.toString());
    }

    /**
     * 反射获取对象的字段包括私有的
     *
     * @param object    被提取字段的对象
     * @param fieldName 字段名称
     * @return 字段的值
     */
    public static Object getField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * 反射获取父类对象的字段包括私有的
     *
     * @param paramClass 被提取字段的对象
     * @param fieldName  字段名称
     * @return 字段的值
     */
    public static Object getSuperField(Object paramClass, String fieldName) throws Exception {
        Object object = null;
        Field field = paramClass.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        object = field.get(paramClass);
        return object;
    }

    /**
     * 调用某一个类的静态方法
     *
     * @param className  类名
     * @param methodName 方法名称
     * @param paramTypes 参数类型列表
     * @param parameters 参数列表
     * @return 方法返回值
     */
    public static Object invokeStaticMethod(String className, String methodName, Class[] paramTypes, Object... parameters) throws Exception {
        Class clazz = Class.forName(className);
        return invokeMethod(null, clazz, methodName, paramTypes, parameters);
    }

    public static Object invokeMethod(Object object, Class clazz, String methodName, Class[] paramTypes, Object... parameters) throws Exception {
        Method method = clazz.getMethod(methodName, paramTypes);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        return method.invoke(object, parameters);
    }

    public static boolean isPrimitiveType(Object object) {
        try {
            return ((Class<?>) object.getClass().getField("TYPE").get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

}