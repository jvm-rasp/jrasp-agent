package com.jrasp.core.classloader;

import com.jrasp.api.annotation.Stealth;
import com.jrasp.api.spi.ModuleJarUnLoadSpi;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import com.jrasp.api.log.Log;
import com.jrasp.core.log.LogFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;

import static com.jrasp.api.util.GaStringUtils.getJavaClassName;
import static com.jrasp.core.log.AgentLogIdConstant.*;
import static com.jrasp.core.util.RaspReflectUtils.*;

/**
 * 模块类加载器
 */
@Stealth
public class ModuleJarClassLoader extends RoutingURLClassLoader {

    private final Log logger = LogFactory.getLog(getClass());
    // 模块jar文件
    private final File moduleJarFile;
    private final File tempModuleJarFile;
    private final long checksumCRC32;

    private static File copyFileToPidDir(final File moduleJarFile, final File directory) throws IOException {
        if (directory == null) {
            // 目标目录是null，不复制
            return moduleJarFile;
        }
        File targetFile = new File(directory + File.separator + moduleJarFile.getName());
        FileUtils.copyFile(moduleJarFile, targetFile);
        return targetFile;
    }

    public ModuleJarClassLoader(final File moduleJarFile, final File directory, final boolean isCopy) throws IOException {
        this(moduleJarFile, copyFileToPidDir(moduleJarFile, directory));
    }

    private ModuleJarClassLoader(final File moduleJarFile, final File tempModuleJarFile) throws IOException {
        super(
                new URL[]{new URL("file:" + tempModuleJarFile.getPath())},
                new Routing(
                        ModuleJarClassLoader.class.getClassLoader(),
                        "^com\\.jrasp\\.api\\..*",
                        "^jrasp.javax\\.servlet\\..*"
                )
        );
        this.checksumCRC32 = FileUtils.checksumCRC32(moduleJarFile);
        this.moduleJarFile = moduleJarFile;
        this.tempModuleJarFile = tempModuleJarFile;

        try {
            cleanProtectionDomainWhichCameFromModuleJarClassLoader();
            logger.debug(PROTECTION_DOMAIN_LOG_ID, "clean ProtectionDomain in {}'s acc success.", this);
        } catch (Throwable e) {
            logger.warn(PROTECTION_DOMAIN_ERROR_LOG_ID, "clean ProtectionDomain in {}'s acc failed.", this, e);
        }
    }

    /**
     * 清理来自URLClassLoader.acc.ProtectionDomain[]中，来自上一个ModuleJarClassLoader的ProtectionDomain
     * 这样写好蛋疼，而且还有不兼容的风险，从JDK6+都必须要这样清理，但我找不出更好的办法。
     * 在重置沙箱时，遇到MgrModule模块无法正确卸载类的情况，主要的原因是在于URLClassLoader.acc.ProtectionDomain[]中包含了上一个ModuleJarClassLoader的引用
     * 所以必须要在这里清理掉，否则随着重置次数的增加，类会越累积越多
     */
    private void cleanProtectionDomainWhichCameFromModuleJarClassLoader() {

        // got ProtectionDomain[] from URLClassLoader's acc
        final AccessControlContext acc = unCaughtGetClassDeclaredJavaFieldValue(URLClassLoader.class, "acc", this);
        final ProtectionDomain[] protectionDomainArray = unCaughtInvokeMethod(
                unCaughtGetClassDeclaredJavaMethod(AccessControlContext.class, "getContext"),
                acc
        );

        // remove ProtectionDomain which loader is ModuleJarClassLoader
        final Set<ProtectionDomain> cleanProtectionDomainSet = new LinkedHashSet<ProtectionDomain>();
        if (ArrayUtils.isNotEmpty(protectionDomainArray)) {
            for (final ProtectionDomain protectionDomain : protectionDomainArray) {
                if (protectionDomain.getClassLoader() == null
                        || !StringUtils.equals(ModuleJarClassLoader.class.getName(), protectionDomain.getClassLoader().getClass().getName())) {
                    cleanProtectionDomainSet.add(protectionDomain);
                }
            }
        }

        // rewrite acc
        final AccessControlContext newAcc = new AccessControlContext(cleanProtectionDomainSet.toArray(new ProtectionDomain[]{}));
        unCaughtSetClassDeclaredJavaFieldValue(URLClassLoader.class, "acc", this, newAcc);

    }

    private void onJarUnLoadCompleted() {
        try {
            final ServiceLoader<ModuleJarUnLoadSpi> moduleJarUnLoadSpiServiceLoader
                    = ServiceLoader.load(ModuleJarUnLoadSpi.class, this);
            for (final ModuleJarUnLoadSpi moduleJarUnLoadSpi : moduleJarUnLoadSpiServiceLoader) {
                logger.info(UNLOADING_JAR_LOG_ID, "unloading module-jar: onJarUnLoadCompleted() loader={};moduleJarUnLoadSpi={};",
                        this,
                        getJavaClassName(moduleJarUnLoadSpi.getClass())
                );
                moduleJarUnLoadSpi.onJarUnLoadCompleted();
            }
        } catch (Throwable cause) {
            logger.warn(UNLOADING_JAR_ERROR_LOG_ID, "unloading module-jar: onJarUnLoadCompleted() occur error! loader={};", this, cause);
        }
    }

    public void closeIfPossible() {
        onJarUnLoadCompleted();
        try {

            // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
            if (this instanceof Closeable) {
                logger.debug(CLOSE_JAR_LOG_ID, "JDK is 1.7+, use URLClassLoader[file={}].close()", moduleJarFile);
                try {
                    ((Closeable) this).close();
                } catch (Throwable cause) {
                    logger.warn(CLOSE_JAR_ERROR_LOG_ID, "close ModuleJarClassLoader[file={}] failed. JDK7+", moduleJarFile, cause);
                }
                return;
            }


            // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
            // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
            try {
                logger.debug(REFLECT_CLOSE_JAR_LOG_ID, "JDK is less then 1.7+, use File.release()");
                final Object sun_misc_URLClassPath = unCaughtGetClassDeclaredJavaFieldValue(URLClassLoader.class, "ucp", this);
                final Object java_util_Collection = unCaughtGetClassDeclaredJavaFieldValue(sun_misc_URLClassPath.getClass(), "loaders", sun_misc_URLClassPath);

                for (Object sun_misc_URLClassPath_JarLoader :
                        ((Collection) java_util_Collection).toArray()) {
                    try {
                        final JarFile java_util_jar_JarFile = unCaughtGetClassDeclaredJavaFieldValue(
                                sun_misc_URLClassPath_JarLoader.getClass(),
                                "jar",
                                sun_misc_URLClassPath_JarLoader
                        );
                        java_util_jar_JarFile.close();
                    } catch (Throwable t) {
                        // if we got this far, this is probably not a JAR loader so skip it
                    }
                }

            } catch (Throwable cause) {
                logger.warn(REFLECT_CLOSE_JAR_ERROR_LOG_ID, "close ModuleJarClassLoader[file={}] failed. probably not a HOTSPOT VM", moduleJarFile, cause);
            }

        } finally {

            // 在这里删除掉临时文件
            FileUtils.deleteQuietly(tempModuleJarFile);

        }

    }

    public File getModuleJarFile() {
        return moduleJarFile;
    }

    @Override
    public String toString() {
        return String.format("ModuleJarClassLoader[crc32=%s;file=%s;]", checksumCRC32, moduleJarFile);
    }

    public long getChecksumCRC32() {
        return checksumCRC32;
    }

}
