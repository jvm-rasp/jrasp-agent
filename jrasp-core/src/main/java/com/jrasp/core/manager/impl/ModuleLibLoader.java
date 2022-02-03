package com.jrasp.core.manager.impl;

import com.jrasp.api.Information;
import com.jrasp.api.log.Log;
import com.jrasp.core.log.LogFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;

import static com.jrasp.core.log.AgentLogIdConstant.AGENT_COMMON_LOG_ID;
import static org.apache.commons.io.FileUtils.convertFileCollectionToFileArray;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * 模块目录加载器
 * 用于从${module.lib}中加载所有的沙箱模块
 */
public class ModuleLibLoader {

    private final Log logger = LogFactory.getLog(getClass());

    // 模块加载目录
    private final File moduleLibDir;

    private final File copyDir;

    // 沙箱加载模式
    private final Information.Mode mode;

    public ModuleLibLoader(final File moduleLibDir, File copyDir, final Information.Mode mode) {
        this.moduleLibDir = moduleLibDir;
        this.copyDir = copyDir;
        this.mode = mode;
    }

    /**
     * 加载Module
     *
     * @param mjCb 模块文件加载回调
     * @param mCb  模块加载回掉
     */
    void load(final ModuleJarLoadCallback mjCb, final ModuleJarLoader.ModuleLoadCallback mCb) {
        // 依次加载
        for (final File moduleJarFile : listModuleJarFileInLib()) {
            try {
                mjCb.onLoad(moduleJarFile);  // todo, 这个回调的实现是空的，暂时不起作用
                ModuleJarLoader moduleJarLoader = new ModuleJarLoader(moduleJarFile, copyDir, mode);
                moduleJarLoader.load(mCb);  // 类加载回调
            } catch (Throwable cause) {
                logger.warn(AGENT_COMMON_LOG_ID,"loading module-jar occur error! module-jar={};", moduleJarFile, cause);
            }
        }
    }

    // 获取全部jar文件
    private File[] listModuleJarFileInLib() {
        final File[] moduleJarFileArray = toModuleJarFileArray();
        Arrays.sort(moduleJarFileArray);   // todo 排序，输出日志jar信息是按照字母排序的
        logger.info(AGENT_COMMON_LOG_ID,"loading module-lib={}, found {} module-jar files : {}",
                moduleLibDir,
                moduleJarFileArray.length,
                join(moduleJarFileArray, ",")    // list转字符串逗号分割
        );
        return moduleJarFileArray;
    }

    // 获取指定目录下的所有"xxx.jar"文件，兼容文件和目录的形式
    private File[] toModuleJarFileArray() {
        if (moduleLibDir.exists()
                && moduleLibDir.isFile()
                && moduleLibDir.canRead()
                && StringUtils.endsWith(moduleLibDir.getName(), ".jar")) {
            return new File[]{moduleLibDir};
        } else {
            return convertFileCollectionToFileArray(listFiles(moduleLibDir, new String[]{"jar"}, false));
        }
    }

    /**
     * 模块文件加载回调
     */
    public interface ModuleJarLoadCallback {

        /**
         * 模块文件加载回调
         *
         * @param moduleJarFile 模块文件
         * @throws Throwable 加载回调异常
         */
        void onLoad(File moduleJarFile) throws Throwable;

    }

}
