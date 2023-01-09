package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.annotation.Information;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.io.FileUtils.convertFileCollectionToFileArray;
import static org.apache.commons.io.FileUtils.listFiles;

/**
 * 模块目录加载器
 * 用于从${module.lib}中加载所有的沙箱模块
 * Created by luanjia@taobao.com on 2016/11/17.
 */
class ModuleLibLoader {

    private final static Logger logger = Logger.getLogger(ModuleLibLoader.class.getName());

    // 模块加载目录
    private final File moduleLibDir;

    private final String copyDir;

    // 沙箱加载模式
    private final Information.Mode mode;

    ModuleLibLoader(final File moduleLibDir, final String copyDir,
                    final Information.Mode mode) {
        this.moduleLibDir = moduleLibDir;
        this.mode = mode;
        this.copyDir = copyDir;
    }

    void load(final ModuleJarLoader.ModuleLoadCallback mCb) {
        long start = System.currentTimeMillis() / 1000;
        // TODO 优化 并发加载
        File[] allJar = listModuleJarFileInLib();
        logger.log(Level.INFO, "load all module, total: {0}, list: {1} ", new Object[]{allJar.length, jarList(allJar)});
        for (final File moduleJarFile : allJar) {
            try {
                new ModuleJarLoader(moduleJarFile, copyDir, mode).load(mCb);
            } catch (Throwable cause) {
                logger.log(Level.WARNING, "loading module-jar occur error! module-jar=" + moduleJarFile, cause);
            }
        }
        long end = System.currentTimeMillis() / 1000;
        logger.log(Level.INFO, "all module load time: " + (end - start) + " ms");
    }

    private File[] listModuleJarFileInLib() {
        return convertFileCollectionToFileArray(listFiles(moduleLibDir, new String[]{"jar"}, false));
    }

    private String jarList(File[] allJar) {
        final StringBuilder buf = new StringBuilder(64);
        for (int i = 0; i < allJar.length ; i++) {
            if (i > 0) {
                buf.append(",");
            }
            if (allJar[i] != null) {
                buf.append(allJar[i].getName());
            }
        }
        return buf.toString();
    }
}
