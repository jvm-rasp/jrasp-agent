package com.jrasp.agent.core.manager;

import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.core.enhance.EventEnhancer;
import com.jrasp.agent.core.newlog.LogUtil;
import com.jrasp.agent.core.util.RaspClassUtils;
import com.jrasp.agent.core.util.SandboxProtector;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 沙箱类形变器
 *
 * @author luanjia@taobao.com
 * @author jrasp
 */
public class RaspClassFileTransformer implements ClassFileTransformer {

    // 保存全局需要转换的目标类、方法、方法对应的adviceListener
    public Map<String, ClassMatcher> targetClazzMap = new ConcurrentHashMap<String, ClassMatcher>();

    public static final RaspClassFileTransformer INSTANCE = new RaspClassFileTransformer();

    // TODO 线程安全的set
    public static final Set<String> transformedClass = new ConcurrentSkipListSet<String>();

    private final boolean isNativeMethodEnhanceSupported = true;

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String internalClassName,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] srcByteCodeArray) {

        SandboxProtector.instance.enterProtecting();
        try {
            // 这里过滤掉Sandbox所需要的类|来自SandboxClassLoader所加载的类|来自ModuleJarClassLoader加载的类
            // 防止ClassCircularityError的发生
            // TODO 这里需要增强 做到可以配置
            if (RaspClassUtils.isComeFromRaspFamily(internalClassName, loader)) {
                return null;
            }
            return _transform(loader, internalClassName, srcByteCodeArray);
        } catch (Throwable cause) {
            LogUtil.warning("sandbox transform " + internalClassName + " failed,will ignore this transform.", cause);
            return null;
        } finally {
            SandboxProtector.instance.exitProtecting();
        }
    }

    // 进入 transform 方法意味着当前jvm需要进入到 safepoint
    // 对于不需要增强的类，立即返回即可，这里不要执行太多判断逻辑
    private byte[] _transform(final ClassLoader loader, final String internalClassName, final byte[] srcByteCodeArray) {
        if (internalClassName == null) {
            return null;
        }
        ClassMatcher classMatcher = targetClazzMap.get(internalClassName);
        if (null == classMatcher) {
            return null;
        }

        // 开始进行类匹配
        try {
            final byte[] toByteCodeArray = new EventEnhancer(isNativeMethodEnhanceSupported).toByteCodeArray(loader, srcByteCodeArray, classMatcher);
            if (srcByteCodeArray == toByteCodeArray) {
                LogUtil.info("transform ignore " + internalClassName + ", nothing changed in loader=" + loader);
                return null;
            }
            transformedClass.add(internalClassName);
            return toByteCodeArray;
        } catch (Throwable cause) {
            LogUtil.warning("transform: " + internalClassName + " failed, in loader: " + loader, cause);
            return null;
        }
    }
}
