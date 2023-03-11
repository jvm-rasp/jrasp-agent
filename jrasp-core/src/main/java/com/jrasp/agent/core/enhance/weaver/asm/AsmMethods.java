package com.jrasp.agent.core.enhance.weaver.asm;


import org.objectweb.asm.commons.Method;

import java.com.jrasp.agent.bridge110.Spy;

import static com.jrasp.agent.core.enhance.weaver.asm.AsmMethods.InnerHelper.getAsmMethod;
import static com.jrasp.agent.core.util.RaspReflectUtils.unCaughtGetClassDeclaredJavaMethod;

/**
 * 常用的ASM method 集合
 * 省得我到处声明
 * Created by luanjia@taobao.com on 16/5/21.
 */
public interface AsmMethods {

    class InnerHelper {
        private InnerHelper() {
        }

        static Method getAsmMethod(final Class<?> clazz,
                                   final String methodName,
                                   final Class<?>... parameterClassArray) {
            return Method.getMethod(unCaughtGetClassDeclaredJavaMethod(clazz, methodName, parameterClassArray));
        }
    }

    /**
     * asm method of {@link Spy#spyMethodOnBefore(Object[], String, int, int, String, String, String, Object)}
     */
    Method ASM_METHOD_Spy$spyMethodOnBefore = getAsmMethod(
            Spy.class,
            "spyMethodOnBefore",
            Object[].class, String.class, int.class, int.class, String.class, String.class, String.class, Object.class
    );

    /**
     * asm method of {@link Spy#spyMethodOnReturn(Object, String, int, int)}
     */
    Method ASM_METHOD_Spy$spyMethodOnReturn = getAsmMethod(Spy.class, "spyMethodOnReturn", Object.class, String.class, int.class, int.class);

    /**
     * asm method of {@link Spy#spyMethodOnThrows(Throwable, String, int, int)}
     */
    Method ASM_METHOD_Spy$spyMethodOnThrows = getAsmMethod(
            Spy.class,
            "spyMethodOnThrows",
            Throwable.class, String.class, int.class, int.class
    );
}
