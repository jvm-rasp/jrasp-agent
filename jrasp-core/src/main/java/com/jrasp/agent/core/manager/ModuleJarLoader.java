package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.core.classloader.ModuleJarClassLoader;
import com.jrasp.agent.core.util.array.ArrayUtils;
import com.jrasp.agent.core.util.string.RaspStringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class ModuleJarLoader {

    private final static Logger logger = Logger.getLogger(ModuleJarLoader.class.getName());

    // 等待加载的模块jar文件
    private final File moduleJarFile;

    private final String copyDir;

    private final String key;

    // 沙箱加载模式
    private final Information.Mode mode;

    ModuleJarLoader(final File moduleJarFile, final String copyDir, final String key,
                    final Information.Mode mode) {
        this.moduleJarFile = moduleJarFile;
        this.mode = mode;
        this.key = key;
        this.copyDir = copyDir;
    }

    private boolean loadingModules(final ModuleJarClassLoader moduleClassLoader,
                                   final ModuleLoadCallback mCb) {

        final Set<String> loadedModuleUniqueIds = new LinkedHashSet<String>();
        final ServiceLoader<Module> moduleServiceLoader = ServiceLoader.load(Module.class, moduleClassLoader);
        final Iterator<Module> moduleIt = moduleServiceLoader.iterator();
        while (moduleIt.hasNext()) {

            final Module module;
            try {
                module = moduleIt.next();
            } catch (Throwable cause) {
                logger.log(Level.WARNING, "loading module instance failed: instance occur error, will be ignored. module-jar=" + moduleJarFile, cause);
                continue;
            }

            final Class<?> classOfModule = module.getClass();

            // 判断模块是否实现了@Information标记
            if (!classOfModule.isAnnotationPresent(Information.class)) {
                logger.log(Level.WARNING, "loading module instance failed: not implements @Information, will be ignored. class={0};module-jar={1};",
                        new Object[]{classOfModule, moduleJarFile}
                );
                continue;
            }

            final Information info = classOfModule.getAnnotation(Information.class);
            final String uniqueId = info.id();

            // 判断模块ID是否合法
            if (RaspStringUtils.isBlank(uniqueId)) {
                logger.log(Level.WARNING, "loading module instance failed: @Information.id is missing, will be ignored. class={0};module-jar={1};",
                        new Object[]{classOfModule, moduleJarFile}
                );
                continue;
            }

            // 判断模块要求的启动模式和容器的启动模式是否匹配
            if (!ArrayUtils.contains(info.mode(), mode)) {
                logger.log(Level.WARNING, "loading module instance failed: launch-mode is not match module required, will be ignored. " +
                                "module={0};launch-mode={1};required-mode={2};class={3};module-jar={4};",
                        new Object[]{uniqueId,
                                mode,
                                RaspStringUtils.join(info.mode(), ","),
                                classOfModule,
                                moduleJarFile}
                );
                continue;
            }

            try {
                if (null != mCb) {
                    mCb.onLoad(uniqueId, classOfModule, module, moduleJarFile, moduleClassLoader);
                }
            } catch (Throwable cause) {
                logger.log(Level.WARNING, "loading module instance failed: MODULE-LOADER-PROVIDER denied, will be ignored.",
                        cause
                );
                continue;
            }

            loadedModuleUniqueIds.add(uniqueId);

        }

        return !loadedModuleUniqueIds.isEmpty();
    }


    void load(final ModuleLoadCallback mCb) throws IOException {

        boolean hasModuleLoadedSuccessFlag = false;
        ModuleJarClassLoader moduleJarClassLoader = null;
        try {
            moduleJarClassLoader = new ModuleJarClassLoader(moduleJarFile, copyDir, key);

            final ClassLoader preTCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(moduleJarClassLoader);

            try {
                hasModuleLoadedSuccessFlag = loadingModules(moduleJarClassLoader, mCb);
            } finally {
                Thread.currentThread().setContextClassLoader(preTCL);
            }

        } finally {
            if (!hasModuleLoadedSuccessFlag
                    && null != moduleJarClassLoader) {
                logger.log(Level.WARNING, "[{0}] not found any module , will be close.", moduleJarFile.getName());
                moduleJarClassLoader.closeIfPossible();
            }
        }

    }

    /**
     * 模块加载回调
     */
    public interface ModuleLoadCallback {

        /**
         * 模块加载回调
         *
         * @param uniqueId          模块ID
         * @param moduleClass       模块类
         * @param module            模块实例
         * @param moduleJarFile     模块所在Jar文件
         * @param moduleClassLoader 负责加载模块的ClassLoader
         * @throws Throwable 加载回调异常
         */
        void onLoad(String uniqueId,
                    Class moduleClass,
                    Module module,
                    File moduleJarFile,
                    ModuleJarClassLoader moduleClassLoader) throws Throwable;

    }

}
