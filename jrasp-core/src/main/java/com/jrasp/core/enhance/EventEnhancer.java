package com.jrasp.core.enhance;

import com.jrasp.api.event.Event;
import com.jrasp.api.log.Log;
import com.jrasp.core.enhance.weaver.EventListenerHandler;
import com.jrasp.core.enhance.weaver.asm.EventWeaver;
import com.jrasp.core.log.LogFactory;
import com.jrasp.core.manager.NativeMethodEnhanceAware;
import com.jrasp.core.util.AsmUtils;
import com.jrasp.core.util.ObjectIDs;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static com.jrasp.core.log.AgentLogIdConstant.*;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ASM7;

/**
 * 事件代码增强器
 */
public class EventEnhancer implements Enhancer {

    private final static Log logger = LogFactory.getLog(EventListenerHandler.class);

    private NativeMethodEnhanceAware nativeMethodEnhanceAware;

    public EventEnhancer(NativeMethodEnhanceAware nativeMethodEnhanceAware) {
        this.nativeMethodEnhanceAware = nativeMethodEnhanceAware;
    }

    /**
     * 创建ClassWriter for asm
     *
     * @param cr ClassReader
     * @return ClassWriter
     */
    private ClassWriter createClassWriter(final ClassLoader targetClassLoader,
                                          final ClassReader cr) {
        return new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS) {

            /*
             * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
             * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
             * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
             * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
             *
             * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
             * 规避了原有代码采用Object.class.getClassLoader()的方式
             */
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                return AsmUtils.getCommonSuperClass(type1, type2, targetClassLoader);
            }

        };
    }

    private static final boolean isDumpClass = false;

    /*
     * dump class to file
     * 用于代码调试
     */
    private static byte[] dumpClassIfNecessary(String className, byte[] data) {
        if (!isDumpClass) {
            return data;
        }
        final File dumpClassFile = new File("./rasp-class-dump/" + className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs()
                && !classPath.exists()) {
            logger.warn(DUMP_CLASS_DIR_ERROR_LOG_ID,"create dump classpath={} failed.", classPath);
            return data;
        }

        // 将类字节码写入文件
        try {
            writeByteArrayToFile(dumpClassFile, data);
            logger.info(WRITER_DUMP_CLASS_LOG_ID,"dump {} to {} success.", className, dumpClassFile);
        } catch (IOException e) {
            logger.warn(WRITER_DUMP_CLASS_ERROR_LOG_ID,"dump {} to {} failed.", className, dumpClassFile, e);
        }

        return data;
    }

    @Override
    public byte[] toByteCodeArray(final ClassLoader targetClassLoader,
                                  final byte[] byteCodeArray,
                                  final Set<String> signCodes,
                                  final String namespace,
                                  final int listenerId,
                                  final Event.Type[] eventTypeArray) {
        // 返回增强后字节码
        final ClassReader cr = new ClassReader(byteCodeArray);
        final ClassWriter cw = createClassWriter(targetClassLoader, cr);
        final int targetClassLoaderObjectID = ObjectIDs.instance.identity(targetClassLoader);
        cr.accept(
                new EventWeaver(nativeMethodEnhanceAware,
                        ASM7, cw, namespace, listenerId,
                        targetClassLoaderObjectID,
                        cr.getClassName(),
                        signCodes,
                        eventTypeArray
                ),
                EXPAND_FRAMES
        );
        return dumpClassIfNecessary(cr.getClassName(), cw.toByteArray());
    }

}
