package com.jrasp.agent.core;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.core.classloader.ModuleJarClassLoader;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 沙箱模块内核封装对象
 *
 * @author luanjia@taobao.com
 */
public class CoreModule {

    private final static Logger logger = Logger.getLogger(CoreModule.class.getName());

    // 全局唯一编号
    private final String uniqueId;

    // 模块归属Jar文件
    private final File jarFile;

    // 模块加载的ClassLoader
    private final ModuleJarClassLoader loader;

    // 模块
    private final Module module;

    // 模块所持有的可释放资源
    private final List<ReleaseResource<?>> releaseResources = new ArrayList<ReleaseResource<?>>();

    // TODO
    private Set<ClassMatcher> classMatchers = new CopyOnWriteArraySet<ClassMatcher>();

    // 是否已经激活
    private boolean isActivated;

    // 是否已被加载
    private boolean isLoaded;

    /**
     * 模块业务对象
     *
     * @param uniqueId 模块ID
     * @param jarFile  模块归属Jar文件
     * @param loader   模块加载ClassLoader
     * @param module   模块
     */
    public CoreModule(final String uniqueId,
                      final File jarFile,
                      final ModuleJarClassLoader loader,
                      final Module module) {
        this.uniqueId = uniqueId;
        this.jarFile = jarFile;
        this.loader = loader;
        this.module = module;
    }

    /**
     * 判断模块是否已被激活
     *
     * @return TRUE:已激活;FALSE:未激活
     */
    public boolean isActivated() {
        return isActivated;
    }

    /**
     * 标记模块激活状态
     *
     * @param isActivated 模块激活状态
     * @return this
     */
    public CoreModule markActivated(boolean isActivated) {
        this.isActivated = isActivated;
        return this;
    }

    /**
     * 判断模块是否已经被加载
     *
     * @return TRUE:被加载;FALSE:未被加载
     */
    public boolean isLoaded() {
        return isLoaded;
    }


    /**
     * 标记模块加载状态
     *
     * @param isLoaded 模块加载状态
     * @return this
     */
    public CoreModule markLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
        return this;
    }

    /**
     * 获取ModuleJar文件
     *
     * @return ModuleJar文件
     */
    public File getJarFile() {
        return jarFile;
    }

    /**
     * 获取对应的ModuleJarClassLoader
     *
     * @return ModuleJarClassLoader
     */
    public ModuleJarClassLoader getLoader() {
        return loader;
    }

    /**
     * 获取模块ID
     *
     * @return 模块ID
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * 获取模块实例
     *
     * @return 模块实例
     */
    public Module getModule() {
        return module;
    }

    public Set<ClassMatcher> getClassMatchers() {
        return classMatchers;
    }

    @Override
    public String toString() {
        return String.format("module[id=%s;class=%s;]", uniqueId, module.getClass());
    }


    /**
     * 在模块下追加一个可释放资源
     *
     * @param resource 可释放资源封装
     * @param <T>      资源实体
     * @return 资源实体本身
     */
    public <T> T append(ReleaseResource<T> resource) {
        if (null == resource
                || null == resource.get()) {
            return null;
        }
        synchronized (releaseResources) {
            releaseResources.add(resource);
        }
        logger.log(Level.FINE, "append resource={0} in module[id={1};]", new Object[]{resource.get(), uniqueId});
        return resource.get();
    }

    /**
     * 在当前模块下移除所有可释放资源
     */
    public void releaseAll() {
        synchronized (releaseResources) {
            final Iterator<ReleaseResource<?>> resourceRefIt = releaseResources.iterator();
            while (resourceRefIt.hasNext()) {
                final ReleaseResource<?> resourceRef = resourceRefIt.next();
                resourceRefIt.remove();
                if (null != resourceRef) {
                    logger.log(Level.FINE, "release resource={0} in module={1}", new Object[]{resourceRef.get(), uniqueId});
                    try {
                        resourceRef.release();
                    } catch (Exception cause) {
                        logger.log(Level.WARNING, "release resource occur error in module=" + uniqueId + ";", cause);
                    }
                }
            }
        }
    }

    /**
     * 可释放资源
     *
     * @param <T> 资源类型
     */
    public static abstract class ReleaseResource<T> {

        // 资源弱引用，允许被GC回收
        private final WeakReference<T> reference;

        /**
         * 构造释放资源
         *
         * @param resource 资源目标
         */
        public ReleaseResource(T resource) {
            this.reference = new WeakReference<T>(resource);
        }

        /**
         * 释放资源
         */
        public abstract void release();

        /**
         * 获取资源实体
         *
         * @return 资源实体
         */
        public T get() {
            return reference.get();
        }

    }

}
