package com.jrasp.agent;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarFile;

public class RaspClassLoader extends URLClassLoader {

    private final String toString;
    private final String path;

    RaspClassLoader(final String namespace,
                       final String raspCoreJarFilePath) throws MalformedURLException {
        super(new URL[]{new URL("file:" + raspCoreJarFilePath)});
        this.path = raspCoreJarFilePath;
        this.toString = String.format("RaspClassLoader[namespace=%s;path=%s;]", namespace, path);
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (null != url) {
            return url;
        }
        url = super.getResource(name);
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> urls = findResources(name);
        if (null != urls) {
            return urls;
        }
        urls = super.getResources(name);
        return urls;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        final Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            return super.loadClass(name, resolve);
        }
    }

    @Override
    public String toString() {
        return toString;
    }


    public void closeIfPossible() {
        // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
        if (this instanceof Closeable) {
            try {
                ((Closeable) this).close();
            } catch (Throwable cause) {
                // ignore...
            }
            return;
        }


        // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
        // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
        try {
            final Object sun_misc_URLClassPath = forceGetDeclaredFieldValue(URLClassLoader.class, "ucp", this);
            final Object java_util_Collection = forceGetDeclaredFieldValue(sun_misc_URLClassPath.getClass(), "loaders", sun_misc_URLClassPath);

            for (final Object sun_misc_URLClassPath_JarLoader :
                    ((Collection) java_util_Collection).toArray()) {
                try {
                    final JarFile java_util_jar_JarFile = forceGetDeclaredFieldValue(sun_misc_URLClassPath_JarLoader.getClass(), "jar", sun_misc_URLClassPath_JarLoader);
                    java_util_jar_JarFile.close();
                } catch (Throwable t) {
                    // if we got this far, this is probably not a JAR loader so skip it
                }
            }

        } catch (Throwable cause) {
            // ignore...
        }
    }

    private <T> T forceGetDeclaredFieldValue(Class<?> clazz, String name, Object target) throws NoSuchFieldException, IllegalAccessException {
        final Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

}
