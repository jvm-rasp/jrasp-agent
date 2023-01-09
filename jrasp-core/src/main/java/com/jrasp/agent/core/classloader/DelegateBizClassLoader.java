package com.jrasp.agent.core.classloader;

public class DelegateBizClassLoader extends ClassLoader {
    public DelegateBizClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(final String javaClassName, final boolean resolve) throws ClassNotFoundException {
        return super.loadClass(javaClassName, resolve);
    }
}
