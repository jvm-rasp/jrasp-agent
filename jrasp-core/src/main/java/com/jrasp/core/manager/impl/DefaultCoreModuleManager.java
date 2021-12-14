package com.jrasp.core.manager.impl;

import com.jrasp.api.*;
import com.jrasp.api.Module;
import com.jrasp.api.authentication.JwtTokenService;
import com.jrasp.api.event.Event;
import com.jrasp.api.log.Log;
import com.jrasp.api.resource.*;
import com.jrasp.core.CoreConfigure;
import com.jrasp.core.CoreModule;
import com.jrasp.core.CoreModule.ReleaseResource;
import com.jrasp.core.classloader.ModuleJarClassLoader;
import com.jrasp.core.enhance.weaver.EventListenerHandler;
import com.jrasp.core.enhance.weaver.EventProcessor;
import com.jrasp.core.log.LogFactory;
import com.jrasp.core.manager.CoreLoadedClassDataSource;
import com.jrasp.core.manager.CoreModuleManager;
import com.jrasp.core.manager.ProviderManager;
import com.jrasp.core.manager.impl.ModuleLibLoader.ModuleJarLoadCallback;
import com.jrasp.core.util.RaspProtector;
import com.jrasp.core.util.RaspReflectUtils;
import com.jrasp.core.util.ThreadUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.jrasp.api.ModuleException.ErrorCode.MODULE_ACTIVE_ERROR;
import static com.jrasp.api.ModuleException.ErrorCode.MODULE_FROZEN_ERROR;
import static com.jrasp.api.ModuleException.ErrorCode.MODULE_LOAD_ERROR;
import static com.jrasp.api.ModuleException.ErrorCode.MODULE_NOT_EXISTED;
import static com.jrasp.api.ModuleException.ErrorCode.MODULE_UNLOAD_ERROR;
import static com.jrasp.core.manager.impl.DefaultCoreModuleManager.ModuleLifeCycleType.*;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

public class DefaultCoreModuleManager implements CoreModuleManager {

    private final Log logger = LogFactory.getLog(getClass());

    private final CoreConfigure cfg;
    private final Instrumentation inst;
    private final CoreLoadedClassDataSource classDataSource;
    private final ProviderManager providerManager;

    // 系统模块目录
    private final File systemModuleLibDir;               // 系统模块目录
    private final File systemModuleLibCopyDir;           // 系统模块Copy目录

    // 必装模块目录
    private final File requiredModuleLibDir;             // 必装模块目录
    private final File requiredModuleLibCopyDir;         // 必装模块Copy目录

    // 可选模块目录
    private final File optionalModuleLibDir;               // 可选模块目录
    private final File optionalModuleLibCopyDir;           // 可选模块Copy目录

    // 已加载的模块集合
    private final Map<String, CoreModule> loadedModuleBOMap = new ConcurrentHashMap<String, CoreModule>();

    private static ThreadLocal<HashMap<String, Object>> requestInfoThreadLocal = new ThreadLocal<HashMap<String, Object>>() {
        @Override
        protected HashMap<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };

    private static ThreadLocal<Boolean> enableCurrentThreadHook = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    /**
     * 模块模块管理
     *
     * @param cfg             模块核心配置
     * @param inst            inst
     * @param classDataSource 已加载类数据源
     * @param providerManager 服务提供者管理器
     */
    public DefaultCoreModuleManager(final CoreConfigure cfg,
                                    final Instrumentation inst,
                                    final CoreLoadedClassDataSource classDataSource,
                                    final ProviderManager providerManager) {
        this.cfg = cfg;
        this.inst = inst;
        this.classDataSource = classDataSource;
        this.providerManager = providerManager;
        // 系统模块
        this.systemModuleLibDir = new File(cfg.getSystemModuleLibPath());
        this.systemModuleLibCopyDir = new File(cfg.getRuntimeSystemModulePath());
        // 必装模块
        this.requiredModuleLibDir = new File(cfg.getUserModuleLibPath());
        this.requiredModuleLibCopyDir = new File(cfg.getRuntimeRequiredModulePath());
        // 非必装模块
        this.optionalModuleLibDir = new File(cfg.getRuntimeOptionalModulePath());  // 非必装模块较为特殊：由rasp-sever复制到运行时目录，这里仅需要监听文件变化
        this.optionalModuleLibCopyDir = null;
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
                logger.warn("loading module occur error when load-completed. module={};", coreModule.getUniqueId(), cause);
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
                                   final Module module,
                                   final File moduleJarFile,
                                   final ModuleJarClassLoader moduleClassLoader) throws ModuleException {

        if (loadedModuleBOMap.containsKey(uniqueId)) {
            logger.debug("module already loaded. module={};", uniqueId);
            return;
        }

        logger.info("loading module, module={};class={};module-jar={};",
                uniqueId,
                module.getClass().getName(),
                moduleJarFile
        );

        // 初始化模块信息
        final CoreModule coreModule = new CoreModule(uniqueId, moduleJarFile, moduleClassLoader, module);

        // 注入@Resource资源
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
            for (final Field resourceField : FieldUtils.getFieldsWithAnnotation(module.getClass(), Resource.class)) {
                final Class<?> fieldType = resourceField.getType();

                // LoadedClassDataSource对象注入
                if (LoadedClassDataSource.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            classDataSource,
                            true
                    );
                }

                // ModuleEventWatcher对象注入
                else if (ModuleEventWatcher.class.isAssignableFrom(fieldType)) {
                    final ModuleEventWatcher moduleEventWatcher = coreModule.append(
                            new ReleaseResource<ModuleEventWatcher>(
                                    RaspProtector.instance.protectProxy(
                                            ModuleEventWatcher.class,
                                            new DefaultModuleEventWatcher(inst, classDataSource, coreModule, cfg.isEnableUnsafe(), cfg.getNamespace())
                                    )
                            ) {
                                @Override
                                public void release() {
                                    logger.info("release all raspClassFileTransformer for module={}", coreModule.getUniqueId());
                                    final ModuleEventWatcher moduleEventWatcher = get();
                                    if (null != moduleEventWatcher) {
                                        for (final RaspClassFileTransformer raspClassFileTransformer
                                                : new ArrayList<RaspClassFileTransformer>(coreModule.getRaspClassFileTransformers())) {
                                            moduleEventWatcher.delete(raspClassFileTransformer.getWatchId());
                                        }
                                    }
                                }
                            });

                    writeField(
                            resourceField,
                            module,
                            moduleEventWatcher,
                            true
                    );
                }

                // ModuleController对象注入
                else if (ModuleController.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            new DefaultModuleController(coreModule, this),
                            true
                    );
                }

                // ModuleManager对象注入
                else if (ModuleManager.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            new DefaultModuleManager(this),
                            true
                    );
                }

                // ConfigInfo注入
                else if (ConfigInfo.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            new DefaultConfigInfo(cfg),
                            true
                    );
                }

                // log 注入
                else if (Log.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            LogFactory.getLog(module.getClass()),
                            true
                    );
                }

                // Instrumentation
                else if (Instrumentation.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            inst,
                            true
                    );
                }
                // ThreadLocalHostInfo注入
                else if (ThreadLocal.class.isAssignableFrom(fieldType)) {
                    final String name = resourceField.getName();
                    if ("requestInfoThreadLocal".equals(name)) {
                        writeField(
                                resourceField,
                                module,
                                requestInfoThreadLocal,
                                true
                        );
                    }
                    // 线程hook开关
                    if ("enableCurrentThreadHook".equals(name)) {
                        writeField(
                                resourceField,
                                module,
                                enableCurrentThreadHook,
                                true
                        );
                    }

                }

                // JwtTokenServiceImpl注入
                else if (JwtTokenService.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            JwtTokenServiceImpl.instance,
                            true
                    );
                }

                // EventMonitor注入
                else if (EventMonitor.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            module,
                            new EventMonitor() {
                                @Override
                                public EventPoolInfo getEventPoolInfo() {
                                    return new EventPoolInfo() {
                                        @Override
                                        public int getNumActive() {
                                            return 0;
                                        }

                                        @Override
                                        public int getNumActive(Event.Type type) {
                                            return 0;
                                        }

                                        @Override
                                        public int getNumIdle() {
                                            return 0;
                                        }

                                        @Override
                                        public int getNumIdle(Event.Type type) {
                                            return 0;
                                        }
                                    };
                                }
                            },
                            true
                    );
                }

                // 其他情况需要输出日志警告
                else {
                    logger.warn("module inject @Resource ignored: field not found. module={};class={};type={};field={};",
                            coreModule.getUniqueId(),
                            coreModule.getModule().getClass().getName(),
                            fieldType.getName(),
                            resourceField.getName()
                    );
                }

            }
        } catch (IllegalAccessException cause) {
            throw new ModuleException(coreModule.getUniqueId(), MODULE_LOAD_ERROR, cause);
        }
    }

    private void markActiveOnLoadIfNecessary(final CoreModule coreModule) throws ModuleException {
        logger.info("active module when OnLoad, module={}", coreModule.getUniqueId());
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
    @Override
    public synchronized CoreModule unload(final CoreModule coreModule,
                                          final boolean isIgnoreModuleException) throws ModuleException {

        if (!coreModule.isLoaded()) {
            logger.debug("module already unLoaded. module={};", coreModule.getUniqueId());
            return coreModule;
        }

        logger.info("unloading module, module={};class={};",
                coreModule.getUniqueId(),
                coreModule.getModule().getClass().getName()
        );

        // 尝试冻结模块
        frozen(coreModule, isIgnoreModuleException);

        // 通知生命周期
        try {
            callAndFireModuleLifeCycle(coreModule, MODULE_UNLOAD);
        } catch (ModuleException meCause) {
            if (isIgnoreModuleException) {
                logger.warn("unload module occur error, ignored. module={};class={};code={};",
                        meCause.getUniqueId(),
                        coreModule.getModule().getClass().getName(),
                        meCause.getErrorCode(),
                        meCause
                );
            } else {
                throw meCause;
            }
        }

        // 清除变量并移除所有监听器
        // EventListenerHandler(单例) ---> mappingOfEventProcessor(全局唯一) ---> (LISTENER_ID,EventProcessor)(每个监听器一个)---> EventProcessor.processRef (每个线程一个)
        List<Thread> threadList = ThreadUtil.getThreadList();
        for (final RaspClassFileTransformer sandboxClassFileTransformer : coreModule.getRaspClassFileTransformers()) {
            int listenerId = sandboxClassFileTransformer.getListenerId();
            final EventListenerHandler eventListenerHandler = EventListenerHandler.getSingleton();
            EventProcessor eventProcessor = eventListenerHandler.get(listenerId);
            // todo 遍历所有线程的实现方式不太友好，但是可以做到完全清除
            for (Thread thread : threadList) {
                // 反射获取 ThreadLocalMap 对象
                Object threadLocalMap = RaspReflectUtils.unCaughtGetClassDeclaredJavaFieldValue(Thread.class, "threadLocals", thread);
                if (null != threadLocalMap) {
                    //  反射获取 ThreadLocalMap类的 remove 方法
                    Method method = RaspReflectUtils.unCaughtGetClassDeclaredJavaMethod(threadLocalMap.getClass(), "remove", ThreadLocal.class);
                    try {
                        RaspReflectUtils.unCaughtInvokeMethod(method, threadLocalMap, eventProcessor.processRef);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            eventListenerHandler.remove(listenerId);
        }

        // 从模块注册表中删除
        loadedModuleBOMap.remove(coreModule.getUniqueId());

        // 标记模块为：已卸载
        coreModule.markLoaded(false);

        // 释放所有可释放资源
        coreModule.releaseAll();

        // 尝试关闭ClassLoader
        closeModuleJarClassLoaderIfNecessary(coreModule.getLoader());

        return coreModule;
    }

    @Override
    public void unloadAll() {

        logger.info("force unloading all loaded modules:{}", loadedModuleBOMap.keySet());

        //先冻结所有模块
        for (final CoreModule coreModule : new ArrayList<CoreModule>(loadedModuleBOMap.values())) {
            try {
                frozen(coreModule, true);
            } catch (ModuleException cause) {
                // 强制卸载不可能出错，这里不对外继续抛出任何异常
                logger.warn("force unloading module occur error! module={};", coreModule.getUniqueId(), cause);
            }
        }

        //释放所有用户线程中的本地变量
        EventListenerHandler.getSingleton().clean();

        // 强制卸载所有模块
        for (final CoreModule coreModule : new ArrayList<CoreModule>(loadedModuleBOMap.values())) {
            try {
                unload(coreModule, true);
            } catch (ModuleException cause) {
                // 强制卸载不可能出错，这里不对外继续抛出任何异常
                logger.warn("force unloading module occur error! module={};", coreModule.getUniqueId(), cause);
            }
        }

    }

    @Override
    public synchronized void active(final CoreModule coreModule) throws ModuleException {

        // 如果模块已经被激活，则直接幂等返回
        if (coreModule.isActivated()) {
            logger.debug("module already activated. module={};", coreModule.getUniqueId());
            return;
        }

        logger.info("active module, module={};class={};module-jar={};",
                coreModule.getUniqueId(),
                coreModule.getModule().getClass().getName(),
                coreModule.getJarFile()
        );

        // 通知生命周期
        callAndFireModuleLifeCycle(coreModule, MODULE_ACTIVE);

        // 激活所有监听器
        for (final RaspClassFileTransformer raspClassFileTransformer : coreModule.getRaspClassFileTransformers()) {
            EventListenerHandler.getSingleton().active(
                    raspClassFileTransformer.getListenerId(),
                    raspClassFileTransformer.getEventListener(),
                    raspClassFileTransformer.getEventTypeArray()
            );
        }

        // 标记模块为：已激活
        coreModule.markActivated(true);
    }

    @Override
    public synchronized void frozen(final CoreModule coreModule,
                                    final boolean isIgnoreModuleException) throws ModuleException {

        // 如果模块已经被冻结(尚未被激活)，则直接幂等返回
        if (!coreModule.isActivated()) {
            logger.debug("module already frozen. module={};", coreModule.getUniqueId());
            return;
        }

        logger.info("frozen module, module={};class={};module-jar={};",
                coreModule.getUniqueId(),
                coreModule.getModule().getClass().getName(),
                coreModule.getJarFile()
        );

        // 通知生命周期
        try {
            callAndFireModuleLifeCycle(coreModule, MODULE_FROZEN);
        } catch (ModuleException meCause) {
            if (isIgnoreModuleException) {
                logger.warn("frozen module occur error, ignored. module={};class={};code={};",
                        meCause.getUniqueId(),
                        coreModule.getModule().getClass().getName(),
                        meCause.getErrorCode(),
                        meCause
                );
            } else {
                throw meCause;
            }
        }

        // 冻结所有监听器
        for (final RaspClassFileTransformer raspClassFileTransformer : coreModule.getRaspClassFileTransformers()) {
            EventListenerHandler.getSingleton().frozen(raspClassFileTransformer.getListenerId());
        }

        // 标记模块为：已冻结
        coreModule.markActivated(false);
    }

    @Override
    public Collection<CoreModule> list() {
        return loadedModuleBOMap.values();
    }

    @Override
    public CoreModule get(String uniqueId) {
        return loadedModuleBOMap.get(uniqueId);
    }

    @Override
    public CoreModule getThrowsExceptionIfNull(String uniqueId) throws ModuleException {
        final CoreModule coreModule = get(uniqueId);
        if (null == coreModule) {
            throw new ModuleException(uniqueId, MODULE_NOT_EXISTED);
        }
        return coreModule;
    }


    private boolean isOptimisticDirectoryContainsFile(final File directory,
                                                      final File child) {
        try {
            return FileUtils.directoryContains(directory, child);
        } catch (IOException cause) {
            // 如果这里能抛出异常，则说明directory或者child发生损坏
            // 需要返回TRUE以此作乐观推断，出错的情况也属于当前目录
            // 这个逻辑没毛病,主要是用来应对USER目录被删除引起IOException的情况
            logger.debug("occur OptimisticDirectoryContainsFile: directory={} or child={} maybe broken.", directory, child, cause);
            return true;
        }
    }

    private boolean isSystemModule(final File child) {
        return isOptimisticDirectoryContainsFile(new File(cfg.getSystemModuleLibPath()), child);
    }

    /**
     * 用户模块文件加载回调
     */
    final private class InnerModuleJarLoadCallback implements ModuleJarLoadCallback {
        @Override
        public void onLoad(File moduleJarFile) throws Throwable {
            providerManager.loading(moduleJarFile);
        }
    }

    /**
     * 用户模块加载回调
     */
    final private class InnerModuleLoadCallback implements ModuleJarLoader.ModuleLoadCallback {
        @Override
        public void onLoad(final String uniqueId,
                           final Class moduleClass,
                           final Module module,
                           final File moduleJarFile,
                           final ModuleJarClassLoader moduleClassLoader) throws Throwable {

            // 如果之前已经加载过了相同ID的模块，则放弃当前模块的加载
            if (loadedModuleBOMap.containsKey(uniqueId)) {
                final CoreModule existedCoreModule = get(uniqueId);
                logger.info("IMLCB: module already loaded, ignore load this module. expected:module={};class={};loader={}|existed:class={};loader={};",
                        uniqueId,
                        moduleClass, moduleClassLoader,
                        existedCoreModule.getModule().getClass().getName(),
                        existedCoreModule.getLoader()
                );
                return;
            }

            // 需要经过ModuleLoadingChain的过滤
            providerManager.loading(
                    uniqueId,
                    moduleClass,
                    module,
                    moduleJarFile,
                    moduleClassLoader
            );

            // 之前没有加载过，这里进行加载
            logger.info("IMLCB: found new module, prepare to load. module={};class={};loader={};",
                    uniqueId,
                    moduleClass,
                    moduleClassLoader
            );

            // 这里进行真正的模块加载
            load(uniqueId, module, moduleJarFile, moduleClassLoader);
        }
    }

    @Override
    public synchronized void flush(final boolean isForce) throws ModuleException {
        if (isForce) {
            forceFlush();
        } else {
            softFlush();
        }
    }

    @Override
    public synchronized CoreModuleManager reset() throws ModuleException {

        logger.info("resetting all loaded modules:{}", loadedModuleBOMap.keySet());

        // 1. 强制卸载所有模块
        unloadAll();
        // 2.加载系统模块、必装模块、非必须模块
        loadModule(systemModuleLibDir, systemModuleLibCopyDir, cfg.getLaunchMode());
        loadModule(requiredModuleLibDir, requiredModuleLibCopyDir, cfg.getLaunchMode());
        loadModule(optionalModuleLibDir, optionalModuleLibCopyDir, cfg.getLaunchMode());
        return this;
    }

    private void loadModule(final File from, final File to, final Information.Mode mode) {
        if (from.exists() && from.canRead()) {
            ModuleLibLoader moduleLibLoader = new ModuleLibLoader(from, to, mode);
            moduleLibLoader.load(new InnerModuleJarLoadCallback(), new InnerModuleLoadCallback());
        } else {
            logger.warn("module-lib not access, ignore flush load this lib. path={}", from);
        }
    }

    /**
     * 关闭ModuleJarClassLoader
     * 如ModuleJarClassLoader所加载上来的所有模块都已经被卸载，则该ClassLoader需要主动进行关闭
     *
     * @param loader 需要被关闭的ClassLoader
     */
    private void closeModuleJarClassLoaderIfNecessary(final ClassLoader loader) {

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
            logger.info("ModuleJarClassLoader={} will be close: all module unloaded.", loader);
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

        logger.info("soft-flushing modules:{}", loadedModuleBOMap.keySet());

        final File systemModuleLibDir = new File(cfg.getSystemModuleLibPath());
        try {
            final ArrayList<Long> checksumCRC32s = new ArrayList<Long>();
            final ArrayList<CoreModule> removeCoreModules = new ArrayList<CoreModule>();
            // 1. 找出所有有变动的文件(add/remove)
            final ArrayList<File> appendJarFiles1 = getAllChangeModuleJar(getUserModuleLibFiles1(), checksumCRC32s); // 系统必装
            final ArrayList<File> appendJarFiles2 = getAllChangeModuleJar(getUserModuleLibFiles2(), checksumCRC32s); // 系统可选

            // 2. 找出所有待卸载的已加载用户模块
            for (final CoreModule coreModule : loadedModuleBOMap.values()) {
                final ModuleJarClassLoader moduleJarClassLoader = coreModule.getLoader();

                // 如果是系统模块目录则跳过
                if (isOptimisticDirectoryContainsFile(systemModuleLibDir, coreModule.getJarFile())) {
                    logger.debug("soft-flushing module: module-jar is in system-lib, will be ignored. module-jar={};system-lib={};",
                            coreModule.getJarFile(),
                            systemModuleLibDir
                    );
                    continue;
                }

                // 如果CRC32已经在这次待加载的集合中，则说明这个文件没有变动，忽略
                if (checksumCRC32s.contains(moduleJarClassLoader.getChecksumCRC32())) {
                    logger.info("soft-flushing module: module-jar already loaded, ignored. module-jar={};CRC32={};",
                            coreModule.getJarFile(),
                            moduleJarClassLoader.getChecksumCRC32()
                    );
                    continue;
                }
                logger.info("soft-flushing module: module-jar is changed, module will be reload/remove. module={};module-jar={};",
                        coreModule.getUniqueId(),
                        coreModule.getJarFile()
                );
                removeCoreModules.add(coreModule);
            }

            // 3. 删除remove
            for (final CoreModule coreModule : removeCoreModules) {
                unload(coreModule, true);
            }

            // 4. 加载必装模块
            for (final File jarFile : appendJarFiles1) {
                new ModuleLibLoader(jarFile, new File(cfg.getRuntimeRequiredModulePath()), cfg.getLaunchMode())
                        .load(new InnerModuleJarLoadCallback(), new InnerModuleLoadCallback());
            }

            // 5. 加载可选模块
            for (final File jarFile : appendJarFiles2) {
                new ModuleLibLoader(jarFile, null, cfg.getLaunchMode())  // todo 重构：可选模块不需要复制，这里代码比较难以理解
                        .load(new InnerModuleJarLoadCallback(), new InnerModuleLoadCallback());
            }
        } catch (Throwable cause) {
            logger.warn("soft-flushing modules: occur error.", cause);
        }

    }

    /**
     * 强制刷新(仅更新用户模块)
     * 对所有已经加载的用户模块进行强行卸载并重新加载
     *
     * @throws ModuleException 模块操作失败
     */
    private void forceFlush() throws ModuleException {

        logger.info("force-flushing modules:{}", loadedModuleBOMap.keySet());

        // 1. 卸载模块
        // 等待卸载的模块集合
        final Collection<CoreModule> waitingUnloadCoreModules = new ArrayList<CoreModule>();

        // 找出所有USER的模块，这些模块都需要卸载一遍
        for (final CoreModule coreModule : loadedModuleBOMap.values()) {
            // 如果判断是属于USER模块目录下的模块，则加入到待卸载模块集合，稍后统一进行卸载
            if (!isSystemModule(coreModule.getJarFile())) {
                waitingUnloadCoreModules.add(coreModule);
            }
        }

        // 记录下即将被卸载的模块ID集合
        if (logger.isInfoEnabled()) {
            final Set<String> uniqueIds = new LinkedHashSet<String>();
            for (final CoreModule coreModule : waitingUnloadCoreModules) {
                uniqueIds.add(coreModule.getUniqueId());
            }
            logger.info("force-flush modules: will be unloading modules : {}", uniqueIds);
        }

        // 强制卸载掉所有等待卸载的模块集合中的模块
        for (final CoreModule coreModule : waitingUnloadCoreModules) {
            unload(coreModule, true);
        }

        // 2. 加载必装模块
        loadModule(requiredModuleLibDir, requiredModuleLibCopyDir, cfg.getLaunchMode());

        // 3. 加载非必装模块
        loadModule(optionalModuleLibDir, optionalModuleLibCopyDir, cfg.getLaunchMode());

    }

    // 找出所以变动的模块
    public ArrayList<File> getAllChangeModuleJar(File[] path, ArrayList<Long> checksumCRC32s) {
        ArrayList<File> files = new ArrayList<File>();
        for (final File jarFile : path) {
            final long checksumCRC32;
            try {
                checksumCRC32 = FileUtils.checksumCRC32(jarFile);
            } catch (IOException cause) {
                logger.warn("soft-flushing module: compute module-jar CRC32 occur error. module-jar={};", jarFile, cause);
                continue;
            }
            checksumCRC32s.add(checksumCRC32);
            // 如果CRC32已经在已加载的模块集合中存在，则说明这个文件没有变动，忽略
            if (isChecksumCRC32Existed(checksumCRC32)) {
                logger.info("soft-flushing module: module-jar is not changed, ignored. module-jar={};CRC32={};", jarFile, checksumCRC32);
                continue;
            }

            logger.info("soft-flushing module: module-jar is changed, will be flush. module-jar={};CRC32={};", jarFile, checksumCRC32);
            files.add(jarFile);
        }
        return files;
    }

    // 获取用户必装模块加载文件
    public File[] getUserModuleLibFiles1() {
        return getUserModuleLibFiles(cfg.getUserModuleLibPath());
    }

    // 获取用户可选模块加载文件
    public File[] getUserModuleLibFiles2() {
        return getUserModuleLibFiles(cfg.getRuntimeOptionalModulePath());
    }

    private synchronized File[] getUserModuleLibFiles(String libPath) {
        final Collection<File> foundModuleJarFiles = new LinkedHashSet<File>();
        final File path = new File(libPath);
        if (path.isDirectory()) {
            foundModuleJarFiles.addAll(FileUtils.listFiles(path, new String[]{"jar"}, false));
        }
        return foundModuleJarFiles.toArray(new File[]{});
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
