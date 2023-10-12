package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.*;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.AdviceAdapterListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.core.CoreConfigure;
import com.jrasp.agent.core.CoreModule;
import com.jrasp.agent.core.CoreModule.ReleaseResource;
import com.jrasp.agent.core.classloader.ModuleJarClassLoader;
import com.jrasp.agent.core.enhance.weaver.EventListenerHandler;
import com.jrasp.agent.core.newlog.LogUtil;
import com.jrasp.agent.core.util.ObjectIDs;
import com.jrasp.agent.core.util.RaspReflectUtils;
import com.jrasp.agent.core.util.SandboxProtector;
import com.jrasp.agent.core.util.ThreadUtil;
import org.apache.commons.io.FileUtils;
import sun.rmi.runtime.Log;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jrasp.agent.api.ModuleException.ErrorCode.*;
import static com.jrasp.agent.core.manager.DefaultCoreModuleManager.ModuleLifeCycleType.*;
import static com.jrasp.agent.core.util.RaspReflectUtils.writeField;

/**
 * 默认的模块管理实现
 * Created by luanjia on 16/10/4.
 */
public class DefaultCoreModuleManager {
    private final CoreConfigure cfg;
    private final Instrumentation inst;
    private final DefaultCoreLoadedClassDataSource classDataSource;

    // 请求上下文
    // 上下文增强：使用 InheritableThreadLocal 代替 ThreadLocal 防止线程注入
    public static ThreadLocal<Context> requestContext = new ThreadLocal<Context>() {
        @Override
        protected Context initialValue() {
            return new Context();
        }
    };

    // 模块目录
    private final File moduleLibDir;

    // 已加载的模块集合
    private final Map<String, CoreModule> loadedModuleBOMap = new ConcurrentHashMap<String, CoreModule>();

    /**
     * 模块模块管理
     *
     * @param cfg             模块核心配置
     * @param inst            inst
     * @param classDataSource 已加载类数据源
     */
    public DefaultCoreModuleManager(final CoreConfigure cfg,
                                    final Instrumentation inst,
                                    final DefaultCoreLoadedClassDataSource classDataSource) {
        this.cfg = cfg;
        this.inst = inst;
        this.classDataSource = classDataSource;
        // 初始化模块目录
        this.moduleLibDir = new File(cfg.getModuleLibPath());
    }

    /*
     * 通知模块生命周期
     */
    private void callAndFireModuleLifeCycle(final CoreModule coreModule, final ModuleLifeCycleType type) throws ModuleException {
        if (coreModule.getModule() instanceof ModuleLifecycle) {
            final ModuleLifecycle moduleLifecycle = (ModuleLifecycle) coreModule.getModule();
            final String uniqueId = coreModule.getUniqueId();
            switch (type) {

                case MODULE_LOAD: {
                    try {
                        moduleLifecycle.onLoad();
                    } catch (Throwable throwable) {
                        throw new ModuleException(uniqueId, MODULE_LOAD_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_UNLOAD: {
                    try {
                        moduleLifecycle.onUnload();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getUniqueId(), MODULE_UNLOAD_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_ACTIVE: {
                    try {
                        moduleLifecycle.onActive();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getUniqueId(), MODULE_ACTIVE_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_FROZEN: {
                    try {
                        moduleLifecycle.onFrozen();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getUniqueId(), MODULE_FROZEN_ERROR, throwable);
                    }
                    break;
                }

            }// switch
        }

        // 这里要对LOAD_COMPLETED事件做特殊处理
        // 因为这个事件处理失败不会影响模块变更行为，只做简单的日志处理
        if (type == MODULE_LOAD_COMPLETED
                && coreModule.getModule() instanceof LoadCompleted) {
            try {
                ((LoadCompleted) coreModule.getModule()).loadCompleted();
            } catch (Throwable cause) {
                LogUtil.warning("loading module occur error when load-completed. module=" + coreModule.getUniqueId(), cause);
            }
        }

    }

    /**
     * 加载并注册模块
     * <p>1. 如果模块已经存在则返回已经加载过的模块</p>
     * <p>2. 如果模块不存在，则进行常规加载</p>
     * <p>3. 如果模块初始化失败，则抛出异常</p>
     *
     * @param uniqueId          模块ID
     * @param module            模块对象
     * @param moduleJarFile     模块所在JAR文件
     * @param moduleClassLoader 负责加载模块的ClassLoader
     * @throws ModuleException 加载模块失败
     */
    private synchronized void load(final String uniqueId,
                                   final String metaInfo,
                                   final Module module,
                                   final File moduleJarFile,
                                   final ModuleJarClassLoader moduleClassLoader) throws ModuleException {

        if (loadedModuleBOMap.containsKey(uniqueId)) {
            LogUtil.info("module already loaded. module=" + uniqueId);
            return;
        }

        // 初始化模块信息
        final CoreModule coreModule = new CoreModule(uniqueId, metaInfo, moduleJarFile, moduleClassLoader, module);

        // 注入@RaspResource资源
        injectResourceOnLoadIfNecessary(coreModule);

        callAndFireModuleLifeCycle(coreModule, MODULE_LOAD);

        // 设置为已经加载
        coreModule.markLoaded(true);

        // 如果模块标记了加载时自动激活，则需要在加载完成之后激活模块
        markActiveOnLoadIfNecessary(coreModule);

        // 注册到模块列表中
        loadedModuleBOMap.put(uniqueId, coreModule);

        // 通知生命周期，模块加载完成
        callAndFireModuleLifeCycle(coreModule, MODULE_LOAD_COMPLETED);

    }

    private void injectResourceOnLoadIfNecessary(final CoreModule coreModule) throws ModuleException {
        try {
            final Module module = coreModule.getModule();
            for (final Field resourceField : RaspReflectUtils.getFieldsWithAnnotation(module.getClass(), RaspResource.class)) {
                final Class<?> fieldType = resourceField.getType();
                // ModuleEventWatcher对象注入
                if (ModuleEventWatcher.class.isAssignableFrom(fieldType)) {
                    final ModuleEventWatcher moduleEventWatcher = getModuleEventWatcher(coreModule);
                    writeField(resourceField, module, moduleEventWatcher, true);
                } else if (AlgorithmManager.class.isAssignableFrom(fieldType)) {
                    // AlgorithmManager 注入
                    writeField(resourceField, module, DefaultAlgorithmManager.instance, true);
                } else if (Instrumentation.class.isAssignableFrom(fieldType)) {
                    // Instrumentation 注入
                    // 在模块中可以使用
                    // @RaspResource
                    // private　Instrumentation inst;
                    writeField(resourceField, module, inst, true);
                } else if (AlgorithmManager.class.isAssignableFrom(fieldType)) {
                    // AlgorithmManager 注入
                    writeField(resourceField, module, DefaultAlgorithmManager.instance, true);
                } else if (ThreadLocal.class.isAssignableFrom(fieldType)) {
                    // ThreadLocalHostInfo注入
                    writeField(resourceField, module, requestContext, true);
                } else if (RaspLog.class.isAssignableFrom(fieldType)) {
                    // RaspLog
                    // 这里使用注入的原因是
                    // 为了兼容tomcat的日志格式,tomcat 使用的getlogger与类加载器有关
                    writeField(resourceField, module, LogUtil.getLogger(), true);
                } else if (RaspConfig.class.isAssignableFrom(fieldType)) {
                    // 全局配置
                    writeField(resourceField, module, RaspConfigImpl.getInstance(), true);
                } else if (String.class.isAssignableFrom(fieldType)) {
                    // metainfo
                    writeField(resourceField, module, coreModule.getMetaInfo(), true);
                } else {
                    // 其他情况需要输出日志警告
                    LogUtil.warning("module inject @RaspResource ignored: field not found. module=" + coreModule.getUniqueId());
                }
            }
        } catch (IllegalAccessException cause) {
            throw new ModuleException(coreModule.getUniqueId(), MODULE_LOAD_ERROR, cause);
        }
    }

    private ModuleEventWatcher getModuleEventWatcher(final CoreModule coreModule) {
        final ModuleEventWatcher moduleEventWatcher = coreModule.append(
                new ReleaseResource<ModuleEventWatcher>(SandboxProtector.instance.protectProxy(ModuleEventWatcher.class,
                        new DefaultModuleEventWatcher(inst, classDataSource, coreModule))
                ) {
                    @Override
                    public void release() {
                        LogUtil.info("reset all hook class for " + coreModule.getUniqueId() + " module");
                        final ModuleEventWatcher moduleEventWatcher = get();
                        if (null != moduleEventWatcher) {
                            moduleEventWatcher.delete(coreModule.getClassMatchers());
                        }
                    }
                });
        return moduleEventWatcher;
    }

    private void markActiveOnLoadIfNecessary(final CoreModule coreModule) throws ModuleException {
        final Information info = coreModule.getModule().getClass().getAnnotation(Information.class);
        if (info.isActiveOnLoad()) {
            active(coreModule);
        }
    }

    /**
     * 卸载并删除注册模块
     * <p>1. 如果模块原本就不存在，则幂等此次操作</p>
     * <p>2. 如果模块存在则尝试进行卸载</p>
     * <p>3. 卸载模块之前会尝试冻结该模块</p>
     *
     * @param coreModule              等待被卸载的模块
     * @param isIgnoreModuleException 是否忽略模块异常
     * @throws ModuleException 卸载模块失败
     */
    public synchronized void unload(final CoreModule coreModule,
                                    final boolean isIgnoreModuleException) throws ModuleException {

        if (!coreModule.isLoaded()) {
            LogUtil.info("module already unLoaded. module=" + coreModule.getUniqueId());
            return;
        }

        LogUtil.info("unload module start: " + coreModule.getUniqueId());

        // 尝试冻结模块
        frozen(coreModule, isIgnoreModuleException);

        // 通知生命周期
        try {
            callAndFireModuleLifeCycle(coreModule, MODULE_UNLOAD);
        } catch (ModuleException meCause) {
            if (isIgnoreModuleException) {
                LogUtil.warning("unload module occur error, ignored.", meCause);
            } else {
                throw meCause;
            }
        }

        // 从模块注册表中删除
        loadedModuleBOMap.remove(coreModule.getUniqueId());

        // 标记模块为：已卸载
        coreModule.markLoaded(false);

        // 释放所有可释放资源
        coreModule.releaseAll();

        // 尝试关闭ClassLoader
        closeClassLoader(coreModule.getLoader());

    }

    public void unloadAll() {

        if (loadedModuleBOMap.size() > 0) {
            // 加载的模块为0时，不打日志
            LogUtil.info("force unloading all loaded modules");
        }

        // 强制卸载所有模块
        for (final CoreModule coreModule : new ArrayList<CoreModule>(loadedModuleBOMap.values())) {
            try {
                unload(coreModule, true);
            } catch (ModuleException cause) {
                // 强制卸载不可能出错，这里不对外继续抛出任何异常
                LogUtil.warning("force unloading module occur error! module=" + coreModule.getUniqueId(), cause);
            }
        }

        // 除去 context 线程变量
        // TODO 不兼容jdk17+
        // 初步解决方案：后面将 requestContext 放到bootclassloader中
        List<Thread> threadList1 = ThreadUtil.getThreadList();
        for (Thread thread : threadList1) {
            cleanThreadLocals(thread);
        }

        // shutdown hook 去除 context
//        try {
//            /**
//             * @see java.lang.ApplicationShutdownHooks#hooks
//             */
//            Class<?> cls = Class.forName("java.lang.ApplicationShutdownHooks");
//            Field hooks = cls.getDeclaredField("hooks");
//            hooks.setAccessible(true);
//            Map<Thread, Thread> identityHashMap = (Map<Thread, Thread>) hooks.get(cls);
//            for (Thread thread : identityHashMap.keySet()) {
//                cleanThreadLocals(thread);
//            }
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "remove context threadLocals in hooks err.", e);
//        }
        LogUtil.info("remove context threadLocals success.");
    }

    /**
     * 在 rasp 退出时清理线程变量，这里使用 inheritableThreadLocals 应该清除 inheritableThreadLocals
     *
     * @see // Thread.inheritableThreadLocals
     * @see // Thread.threadLocals
     */
    private void cleanThreadLocals(Thread thread) {
        Object threadLocals = RaspReflectUtils.unCaughtGetClassDeclaredJavaFieldValue(Thread.class, "threadLocals", thread);
        if (null != threadLocals) {
            //  反射获取 ThreadLocalMap类的 remove 方法
            Method method = RaspReflectUtils.unCaughtGetClassDeclaredJavaMethod(threadLocals.getClass(), "remove", ThreadLocal.class);
            try {
                RaspReflectUtils.unCaughtInvokeMethod(method, threadLocals, requestContext);
            } catch (Exception e) {
                LogUtil.warning("remove context threadLocals err.", e);
            }
        }
    }

    public synchronized void active(final CoreModule coreModule) throws ModuleException {

        // 如果模块已经被激活，则直接幂等返回
        if (coreModule.isActivated()) {
            LogUtil.info("module already activated. module= " + coreModule.getUniqueId());
            return;
        }

        // 通知生命周期
        callAndFireModuleLifeCycle(coreModule, MODULE_ACTIVE);

        // 激活所有监听器
        for (final ClassMatcher classMatcher : coreModule.getClassMatchers()) {
            Map<String, MethodMatcher> methodMatcherMap = classMatcher.getMethodMatcherMap();
            for (Map.Entry<String, MethodMatcher> entry : methodMatcherMap.entrySet()) {
                MethodMatcher methodMatcher = entry.getValue();
                int identity = ObjectIDs.instance.identity(methodMatcher.getAdviceListener());
                EventListenerHandler.getSingleton().active(identity, new AdviceAdapterListener(methodMatcher.getAdviceListener()));
            }
        }

        // 标记模块为：已激活
        coreModule.markActivated(true);
    }

    public synchronized void frozen(final CoreModule coreModule,
                                    final boolean isIgnoreModuleException) throws ModuleException {

        // 如果模块已经被冻结(尚未被激活)，则直接幂等返回
        if (!coreModule.isActivated()) {
            LogUtil.info("module already frozen. module=" + coreModule.getUniqueId());
            return;
        }

        LogUtil.info("frozen module: " + coreModule.getUniqueId());

        // 通知生命周期
        try {
            callAndFireModuleLifeCycle(coreModule, MODULE_FROZEN);
        } catch (ModuleException meCause) {
            if (isIgnoreModuleException) {
                LogUtil.warning("frozen module occur error, ignored. module=" + meCause.getUniqueId() + ";" +
                        "class=" + coreModule.getModule().getClass().getName() + ";code=" + meCause.getErrorCode(), meCause);
            } else {
                throw meCause;
            }
        }

        // 冻结所有监听器
        for (final ClassMatcher classMatcher : coreModule.getClassMatchers()) {
            Map<String, MethodMatcher> methodMatcherMap = classMatcher.getMethodMatcherMap();
            for (Map.Entry<String, MethodMatcher> entry : methodMatcherMap.entrySet()) {
                MethodMatcher methodMatcher = entry.getValue();
                int identity = ObjectIDs.instance.identity(methodMatcher.getAdviceListener());
                EventListenerHandler.getSingleton().frozen(identity);
            }
        }

        // 标记模块为：已冻结
        coreModule.markActivated(false);
    }

    public Collection<CoreModule> list() {
        return loadedModuleBOMap.values();
    }

    public CoreModule get(String uniqueId) {
        return loadedModuleBOMap.get(uniqueId);
    }

    public CoreModule getThrowsExceptionIfNull(String uniqueId) throws ModuleException {
        final CoreModule coreModule = get(uniqueId);
        if (null == coreModule) {
            throw new ModuleException(uniqueId, MODULE_NOT_EXISTED);
        }
        return coreModule;
    }

    /**
     * 用户模块加载回调
     */
    final private class InnerModuleLoadCallback implements ModuleJarLoader.ModuleLoadCallback {
        @Override
        public void onLoad(final String uniqueId,
                           final String metaInfo,
                           final Class moduleClass,
                           final Module module,
                           final File moduleJarFile,
                           final ModuleJarClassLoader moduleClassLoader) throws Throwable {

            // 如果之前已经加载过了相同ID的模块，则放弃当前模块的加载
            if (loadedModuleBOMap.containsKey(uniqueId)) {
                LogUtil.info("IMLCB: module already loaded, ignore load this module. expected:module=" + uniqueId);
                return;
            }
            // 这里进行真正的模块加载
            load(uniqueId, metaInfo, module, moduleJarFile, moduleClassLoader);
        }
    }

    public synchronized void flush(final boolean isForce) throws ModuleException {
        if (isForce) {
            forceFlush();
        } else {
            softFlush();
        }
    }

    /**
     * 执行耗时
     *
     * @return
     * @throws ModuleException
     */
    public synchronized DefaultCoreModuleManager reset() throws ModuleException {

        LogUtil.info("start to reset all loaded modules");

        // 1. 强制卸载所有模块
        unloadAll();

        // 2. 加载所有模块
        // 用户模块加载目录，加载用户模块目录下的所有模块
        // 对模块访问权限进行校验
        if (moduleLibDir.exists() && moduleLibDir.canRead()) {
            new ModuleLibLoader(moduleLibDir, cfg.getDecyptKey(), cfg.getLaunchMode()).load(new InnerModuleLoadCallback());
        } else {
            LogUtil.warning("module-lib not access, ignore flush load this lib. path=" + moduleLibDir);
        }
        return this;
    }

    /**
     * 关闭ModuleJarClassLoader
     * 如ModuleJarClassLoader所加载上来的所有模块都已经被卸载，则该ClassLoader需要主动进行关闭
     *
     * @param loader 需要被关闭的ClassLoader
     */
    private void closeClassLoader(final ClassLoader loader) {

        if (!(loader instanceof ModuleJarClassLoader)) {
            return;
        }

        // 查找已经注册的模块中是否仍然还包含有ModuleJarClassLoader的引用
        boolean hasRef = false;
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            if (loader == coreModule.getLoader()) {
                hasRef = true;
                break;
            }
        }

        if (!hasRef) {
            LogUtil.info(loader + " will be close.");
            ((ModuleJarClassLoader) loader).closeIfPossible();
        }

    }


    private boolean isChecksumCRC32Existed(long checksumCRC32) {
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            if (coreModule.getLoader().getChecksumCRC32() == checksumCRC32) {
                return true;
            }
        }
        return false;
    }

    /**
     * 软刷新
     * 找出有变动的模块文件，有且仅有改变这些文件所对应的模块
     */
    private void softFlush() {

        LogUtil.info("soft-flushing modules:" + loadedModuleBOMap.keySet());

        try {
            final ArrayList<File> appendJarFiles = new ArrayList<File>();
            final ArrayList<CoreModule> removeCoreModules = new ArrayList<CoreModule>();
            final ArrayList<Long> checksumCRC32s = new ArrayList<Long>();

            // 1. 找出所有有变动的文件(add/remove)
            for (final File jarFile : cfg.getModuleLibFiles()) {
                final long checksumCRC32;
                try {
                    checksumCRC32 = FileUtils.checksumCRC32(jarFile);
                } catch (IOException cause) {
                    LogUtil.warning("soft-flushing module: compute module-jar CRC32 occur error. module-jar=" + jarFile, cause);
                    continue;
                }
                checksumCRC32s.add(checksumCRC32);
                // 如果CRC32已经在已加载的模块集合中存在，则说明这个文件没有变动，忽略
                if (isChecksumCRC32Existed(checksumCRC32)) {
                    LogUtil.info("soft-flushing module: module-jar is not changed, ignored. module-jar=" + jarFile.getName());
                    continue;
                }

                LogUtil.info("soft-flushing module: module-jar is changed, will be flush. module-jar=" + jarFile.getName());
                appendJarFiles.add(jarFile);
            }

            // 2. 找出所有待卸载的已加载用户模块
            for (final CoreModule coreModule : loadedModuleBOMap.values()) {
                final ModuleJarClassLoader moduleJarClassLoader = coreModule.getLoader();

                // 如果CRC32已经在这次待加载的集合中，则说明这个文件没有变动，忽略
                if (checksumCRC32s.contains(moduleJarClassLoader.getChecksumCRC32())) {
                    LogUtil.info("soft-flushing module: module-jar already loaded, ignored. module-jar=" + coreModule.getJarFile().getName() + ";CRC32=" + moduleJarClassLoader.getChecksumCRC32());
                    continue;
                }
                LogUtil.info("soft-flushing module: module-jar is changed, module will be reload/remove. module=" + coreModule.getUniqueId() + ";module-jar=" + coreModule.getJarFile().getName());
                removeCoreModules.add(coreModule);
            }

            // 3. 删除remove
            for (final CoreModule coreModule : removeCoreModules) {
                unload(coreModule, true);
            }

            // 4. 加载add
            for (final File jarFile : appendJarFiles) {
                new ModuleLibLoader(jarFile, cfg.getDecyptKey(), cfg.getLaunchMode())
                        .load(new InnerModuleLoadCallback());
            }
        } catch (Throwable cause) {
            LogUtil.warning("soft-flushing modules: occur error.", cause);
        }

    }

    /**
     * 强制刷新
     * 对所有已经加载的用户模块进行强行卸载并重新加载
     *
     * @throws ModuleException 模块操作失败
     */
    private void forceFlush() throws ModuleException {

        LogUtil.info("force-flushing modules:" + loadedModuleBOMap.keySet());

        // 1. 卸载模块
        // 等待卸载的模块集合
        final Collection<CoreModule> waitingUnloadCoreModules = new ArrayList<CoreModule>();

        // 找出所有USER的模块，所以这些模块都卸载了
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            waitingUnloadCoreModules.add(coreModule);
        }

        // 记录下即将被卸载的模块ID集合
        final Set<String> uniqueIds = new LinkedHashSet<String>();
        for (final CoreModule coreModule : waitingUnloadCoreModules) {
            uniqueIds.add(coreModule.getUniqueId());
        }
        LogUtil.info("force-flush modules: will be unloading modules: " + uniqueIds);

        // 强制卸载掉所有等待卸载的模块集合中的模块
        for (final CoreModule coreModule : waitingUnloadCoreModules) {
            unload(coreModule, true);
        }

        // 2. 加载模块
        // 用户模块加载目录，加载用户模块目录下的所有模块
        // 对模块访问权限进行校验
        // 用户模块目录
        if (moduleLibDir.exists() && moduleLibDir.canRead()) {
            LogUtil.info("force-flush modules: module-lib=" + moduleLibDir);
            new ModuleLibLoader(moduleLibDir, cfg.getDecyptKey(), cfg.getLaunchMode())
                    .load(new InnerModuleLoadCallback());
        } else {
            LogUtil.warning("force-flush modules: module-lib can not access, will be ignored. module-lib=" + moduleLibDir);
        }

    }

    /**
     * 模块生命周期类型
     */
    enum ModuleLifeCycleType {

        /**
         * 模块加载
         */
        MODULE_LOAD,

        /**
         * 模块卸载
         */
        MODULE_UNLOAD,

        /**
         * 模块激活
         */
        MODULE_ACTIVE,

        /**
         * 模块冻结
         */
        MODULE_FROZEN,

        /**
         * 模块加载完成
         */
        MODULE_LOAD_COMPLETED
    }

}
