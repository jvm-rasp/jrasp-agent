package com.jrasp.agent.core.classloader;

import com.jrasp.agent.core.newlog.LogUtil;
import com.jrasp.agent.core.util.string.RaspStringUtils;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;

import static com.jrasp.agent.core.util.RaspReflectUtils.*;

/**
 * 模块类加载器
 *
 * @author luanjia@taobao.com
 */
public class ModuleJarClassLoader extends RoutingURLClassLoader {

    private final File moduleJarFile;
    private final long checksumCRC32;

    private static File copyFileToPidDir(final File moduleJarFile, final String directory) throws IOException {
        if (directory == null) {
            // 目标目录是null，不复制
            return moduleJarFile;
        }
        File targetFile = new File(directory + File.separator + moduleJarFile.getName());
        FileUtils.copyFile(moduleJarFile, targetFile);
        return moduleJarFile;
    }

    public ModuleJarClassLoader(final File moduleJarFile, String key) throws IOException {
        super(
                new URL[]{new URL("file:" + moduleJarFile.getPath())},
                key,
                new Routing(ModuleJarClassLoader.class.getClassLoader(), "^com\\.jrasp\\.agent\\.api\\..*")
        );
        this.checksumCRC32 = FileUtils.checksumCRC32(moduleJarFile);
        this.moduleJarFile = moduleJarFile;

        try {
            cleanProtectionDomainWhichCameFromModuleJarClassLoader();
            // LogUtil.info("clean ProtectionDomain acc success. " + this.toString());
        } catch (Throwable e) {
            LogUtil.warning("clean ProtectionDomain in acc failed.", e);
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
        if (protectionDomainArray != null && protectionDomainArray.length > 0) {
            for (final ProtectionDomain protectionDomain : protectionDomainArray) {
                if (protectionDomain.getClassLoader() == null
                        || !RaspStringUtils.equals(ModuleJarClassLoader.class.getName(), protectionDomain.getClassLoader().getClass().getName())) {
                    cleanProtectionDomainSet.add(protectionDomain);
                }
            }
        }

        // rewrite acc
        final AccessControlContext newAcc = new AccessControlContext(cleanProtectionDomainSet.toArray(new ProtectionDomain[]{}));
        unCaughtSetClassDeclaredJavaFieldValue(URLClassLoader.class, "acc", this, newAcc);

    }


    public void closeIfPossible() {
        // 如果是JDK7+的版本, URLClassLoader实现了Closeable接口，直接调用即可
        if (this instanceof Closeable) {
            LogUtil.info("JDK is 1.7+, use URLClassLoader close()" + moduleJarFile);
            try {
                ((Closeable) this).close();
            } catch (Throwable cause) {
                LogUtil.warning("close ModuleJarClassLoader[file=" + moduleJarFile.getName() + "] failed. JDK7+", cause);
            }
            return;
        }


        // 对于JDK6的版本，URLClassLoader要关闭起来就显得有点麻烦，这里弄了一大段代码来稍微处理下
        // 而且还不能保证一定释放干净了，至少释放JAR文件句柄是没有什么问题了
        try {
            LogUtil.info("JDK is less then 1.7+, use File.release()");
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
            LogUtil.warning("close ModuleJarClassLoader[file=" + moduleJarFile.getName() + "] failed. probably not a HOTSPOT VM", cause);
        }
    }

    @Override
    public String toString() {
        return moduleJarFile.getName();
    }

    public long getChecksumCRC32() {
        return checksumCRC32;
    }

}
