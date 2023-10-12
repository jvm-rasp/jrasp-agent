package com.jrasp.agent.core.enhance;

import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.core.enhance.weaver.asm.EventWeaver;
import com.jrasp.agent.core.newlog.LogUtil;
import com.jrasp.agent.core.util.ObjectIDs;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ASM9;

/**
 * 事件代码增强器
 *
 * @author luanjia@taobao.com
 */
public class EventEnhancer {
    private final boolean isNativeMethodEnhanceSupported;

    public EventEnhancer(boolean isNativeMethodEnhanceSupported) {
        this.isNativeMethodEnhanceSupported = isNativeMethodEnhanceSupported;
    }

    /*
     * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
     * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
     * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
     * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
     *
     * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
     * 规避了原有代码采用Object.class.getClassLoader()的方式
     */
    private ClassWriter createClassWriter(ClassReader cr, final ClassLoader targetClassLoader) {
        return new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                Class<?> c, d;
                try {
                    c = Class.forName(type1.replace('/', '.'), false, targetClassLoader);
                    d = Class.forName(type2.replace('/', '.'), false, targetClassLoader);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (c.isAssignableFrom(d)) {
                    return type1;
                }
                if (d.isAssignableFrom(c)) {
                    return type2;
                }
                if (c.isInterface() || d.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        c = c.getSuperclass();
                    } while (!c.isAssignableFrom(d));
                    return c.getName().replace('.', '/');
                }
            }
        };
    }

    private static final boolean isDumpClass = false;

    public byte[] toByteCodeArray(final ClassLoader targetClassLoader, final byte[] byteCodeArray, final ClassMatcher matcher) {
        // 返回增强后字节码
        final ClassReader cr = new ClassReader(byteCodeArray);
        final ClassWriter cw = createClassWriter(cr, targetClassLoader);
        final int objectId = ObjectIDs.instance.identity(targetClassLoader);
        cr.accept(new EventWeaver(isNativeMethodEnhanceSupported,
                        ASM9, cw, "default",
                        objectId,
                        cr.getClassName(),
                        matcher
                ),
                EXPAND_FRAMES
        );
        return dumpClass(cr.getClassName(), cw.toByteArray());
    }

    /*
     * dump class to file
     * 用于代码调试
     */
    private static byte[] dumpClass(String className, byte[] data) {
        if (!isDumpClass) {
            return data;
        }
        final File dumpClassFile = new File("./rasp-class-dump/" + className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs() && !classPath.exists()) {
            LogUtil.info("create dump classpath=" + classPath + " failed.");
            return data;
        }

        // 将类字节码写入文件
        try {
            writeByteArrayToFile(dumpClassFile, data);
            LogUtil.info("dump " + className + " to " + dumpClassFile + " success.");
        } catch (IOException e) {
            LogUtil.warning("dump " + className + " to " + dumpClassFile + " failed.", e);
        }

        return data;
    }
}
