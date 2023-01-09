package com.jrasp.agent.core.classloader;

/**
 * @author zhuangpeng
 * @since 2020/1/15
 */
public class BusinessClassLoaderHolder {

    private static final ThreadLocal<DelegateBizClassLoader> holder = new ThreadLocal<DelegateBizClassLoader>();

    public static void setBusinessClassLoader(ClassLoader classLoader){
        if(null == classLoader){
            return;
        }
        DelegateBizClassLoader delegateBizClassLoader = new DelegateBizClassLoader(classLoader);
        holder.set(delegateBizClassLoader);
    }

    public static void removeBusinessClassLoader(){
        holder.remove();
    }

    public static DelegateBizClassLoader getBusinessClassLoader(){
        return null != holder ? holder.get() : null;
    }

}
