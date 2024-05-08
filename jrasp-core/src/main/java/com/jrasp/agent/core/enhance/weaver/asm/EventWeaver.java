package com.jrasp.agent.core.enhance.weaver.asm;

import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.core.enhance.weaver.CodeLock;
import com.jrasp.agent.core.util.ObjectIDs;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jrasp.agent.core.util.string.RaspStringUtils.toInternalClassName;
import static com.jrasp.agent.core.util.string.RaspStringUtils.toJavaClassName;

/**
 * 方法事件编织者
 * Created by luanjia@taobao.com on 16/7/16.
 */
public class EventWeaver extends ClassVisitor implements Opcodes, AsmTypes, AsmMethods {

    public final static String NATIVE_PREFIX = getNativePrefix();

    private final static Logger logger = Logger.getLogger(EventWeaver.class.getName());

    private final int targetClassLoaderObjectID;
    private final String namespace;
    private final String targetJavaClassName;
    private final List<Method> addMethodNodes = new ArrayList();
    private final boolean isNativeMethodEnhanceSupported;
    private final ClassMatcher classMatcher;

    //生成5-8位的随机NATIVE_PREFIX字符串，防止attacker直接调用$$JRASP$$从而escape native method hook.
    public static String getNativePrefix(){
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        int length = random.nextInt(4)+5; //5-8位长度
        StringBuilder sb = new StringBuilder();
        sb.append("$$");
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            sb.append(randomChar);
        }
        sb.append("$$");
        return sb.toString();
    }

    public EventWeaver(
            final boolean isNativeMethodEnhanceSupported,
            final int api,
            final ClassVisitor cv,
            final String namespace,
            final int targetClassLoaderObjectID,
            final String targetClassInternalName,
            final ClassMatcher matcher) {
        super(api, cv);
        this.isNativeMethodEnhanceSupported = isNativeMethodEnhanceSupported;
        this.targetClassLoaderObjectID = targetClassLoaderObjectID;
        this.namespace = namespace;
        this.targetJavaClassName = toJavaClassName(targetClassInternalName);
        this.classMatcher = matcher;
    }

    private MethodMatcher isMatchedBehavior(final String name$desc) {
        return classMatcher.findMethodMatcher(name$desc);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        MethodMatcher methodMatcher = isMatchedBehavior(name + desc);
        if (null == methodMatcher) {
            // 忽略方法的参数描述符号
            methodMatcher = isMatchedBehavior(name);
            if (null == methodMatcher) {
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        }

        methodMatcher.setHook(true);
        logger.log(Level.CONFIG, "hook method {0} ", methodMatcher.desc());

        // 匹配命中，获取listenerId
        final int listenerId = ObjectIDs.instance.identity(methodMatcher.getAdviceListener());

        if ((access & Opcodes.ACC_NATIVE) != 0) {
            if (!isNativeMethodEnhanceSupported) {
                throw new UnsupportedOperationException("Native Method Prefix Unsupported");
            }
            //native 方法插桩策略：
            //1.原始的native变为非native方法，并增加AOP式方法体
            //2.在AOP中增加调用wrapper后的native方法
            //3.增加wrapper的native方法
            //去掉native
            int newAccess = access & ~Opcodes.ACC_NATIVE;
            final MethodVisitor mv = super.visitMethod(newAccess, name, desc, signature, exceptions);
            return new ReWriteMethod(api, new JSRInlinerAdapter(mv, newAccess, name, desc, signature, exceptions), newAccess, name, desc) {

                private final Label beginLabel = new Label();
                private final Label endLabel = new Label();
                private final Label startCatchBlock = new Label();
                private final Label endCatchBlock = new Label();
                private int newLocal = -1;
                // 代码锁
                private final CodeLock codeLockForTracing = new CallAsmCodeLock(this);

                // 加载ClassLoader
                private void loadClassLoader() {
                    push(targetClassLoaderObjectID);
                }

                /**
                 * 流程控制
                 */

                @Override
                public void visitEnd() {
                    if (!name.startsWith(NATIVE_PREFIX)) {
                        codeLockForTracing.lock(new CodeLock.Block() {
                            @Override
                            public void code() {
                                mark(beginLabel);
                                loadArgArray();
                                dup();
                                push(namespace);
                                push(listenerId);
                                loadClassLoader();
                                push(targetJavaClassName);
                                push(name);
                                push(desc);
                                loadThisOrPushNullIfIsStatic();
                                invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnBefore);
                                swap();
                                storeArgArray();
                                pop();
                                processControl(desc);
                                final String wrapperNativeMethodName = NATIVE_PREFIX + name;
                                Method wrapperMethod = new Method(access, wrapperNativeMethodName, desc);
                                Method fakeWrapperMethod = new Method(access, getNativePrefix() + name, desc); //虚假wrapperMethod
                                String owner = toInternalClassName(targetJavaClassName);
                                if (!isStaticMethod()) {
                                    loadThis();
                                }
                                loadArgs();
                                if (isStaticMethod()) {
                                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, wrapperMethod.getName(), wrapperMethod.getDescriptor(), false);
                                } else {
                                    //wrapper的方法永远都是private
                                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, wrapperMethod.getName(), wrapperMethod.getDescriptor(), false);
                                }
                                EventWeaver.this.addMethodNodes.add(fakeWrapperMethod);    //最终添加至addMethodNodes中。
                                EventWeaver.this.addMethodNodes.add(wrapperMethod);
                                loadReturn(Type.getReturnType(desc));
                                push(namespace);
                                push(listenerId);
                                invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnReturn);
                                processControl(desc, true);
                                returnValue();
                                mark(endLabel);
                                mv.visitLabel(startCatchBlock);
                                visitTryCatchBlock(beginLabel, endLabel, startCatchBlock, ASM_TYPE_THROWABLE.getInternalName());
                                newLocal = newLocal(ASM_TYPE_THROWABLE);
                                storeLocal(newLocal);
                                loadLocal(newLocal);
                                push(namespace);
                                push(listenerId);
                                invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnThrows);
                                processControl(desc);
                                loadLocal(newLocal);
                                throwException();
                                mv.visitLabel(endCatchBlock);
                            }
                        });
                    }
                    super.visitLocalVariable("t", ASM_TYPE_THROWABLE.getDescriptor(), null, startCatchBlock, endCatchBlock,
                            newLocal);
                    super.visitEnd();
                }
            };

        } else {
            final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new ReWriteMethod(api, new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions), access, name, desc) {

                private final Label beginLabel = new Label();
                private final Label endLabel = new Label();
                private final Label startCatchBlock = new Label();
                private final Label endCatchBlock = new Label();
                private int newlocal = -1;

                // 用来标记一个方法是否已经进入
                // JVM中的构造函数非常特殊，super();this();是在构造函数方法体执行之外进行，如果在这个之前进行了任何的流程改变操作
                // 将会被JVM加载类的时候判定校验失败，导致类加载出错
                // 所以这里需要用一个标记为告知后续的代码编织，绕开super()和this()

                // 代码锁
                private final CodeLock codeLockForTracing = new CallAsmCodeLock(this);


                // 加载ClassLoader
                private void loadClassLoader() {
                    push(targetClassLoaderObjectID);
                }

                @Override
                protected void onMethodEnter() {
                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            mark(beginLabel);
                            loadArgArray();
                            dup();
                            push(namespace);
                            push(listenerId);
                            loadClassLoader();
                            push(targetJavaClassName);
                            push(name);
                            push(desc);
                            loadThisOrPushNullIfIsStatic();
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnBefore);
                            swap();
                            storeArgArray();
                            pop();
                            processControl(desc);
                        }
                    });
                }

                /**
                 * 是否抛出异常返回(通过字节码判断)
                 *
                 * @param opcode 操作码
                 * @return true:以抛异常形式返回 / false:非抛异常形式返回(return)
                 */
                private boolean isThrow(int opcode) {
                    return opcode == ATHROW;
                }

                /**
                 * 加载返回值
                 * @param opcode 操作吗
                 */
                private void loadReturn(int opcode) {
                    switch (opcode) {

                        case RETURN: {
                            pushNull();
                            break;
                        }

                        case ARETURN: {
                            dup();
                            break;
                        }

                        case LRETURN:
                        case DRETURN: {
                            dup2();
                            box(Type.getReturnType(methodDesc));
                            break;
                        }

                        default: {
                            dup();
                            box(Type.getReturnType(methodDesc));
                            break;
                        }

                    }
                }

                @Override
                protected void onMethodExit(final int opcode) {
                    if (!isThrow(opcode) && !codeLockForTracing.isLock()) {
                        codeLockForTracing.lock(new CodeLock.Block() {
                            @Override
                            public void code() {
                                loadReturn(opcode);
                                push(namespace);
                                push(listenerId);
                                invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnReturn);
                                //[Ret,rawRespond]
                                processControl(desc, true);
                            }
                        });
                    }
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    mark(endLabel);
                    mv.visitLabel(startCatchBlock);
                    visitTryCatchBlock(beginLabel, endLabel, startCatchBlock, ASM_TYPE_THROWABLE.getInternalName());

                    codeLockForTracing.lock(new CodeLock.Block() {
                        @Override
                        public void code() {
                            newlocal = newLocal(ASM_TYPE_THROWABLE);
                            storeLocal(newlocal);
                            loadLocal(newlocal);
                            push(namespace);
                            push(listenerId);
                            invokeStatic(ASM_TYPE_SPY, ASM_METHOD_Spy$spyMethodOnThrows);
                            processControl(desc);
                            loadLocal(newlocal);
                        }
                    });

                    throwException();
                    mv.visitLabel(endCatchBlock);
                    super.visitMaxs(maxStack, maxLocals);
                }

                @Override
                public void visitInsn(int opcode) {
                    super.visitInsn(opcode);
                    codeLockForTracing.code(opcode);
                }

                // 用于try-catch的重排序
                // 目的是让call的try...catch能在exceptions tables排在前边
                private final ArrayList<AsmTryCatchBlock> asmTryCatchBlocks = new ArrayList<AsmTryCatchBlock>();

                @Override
                public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                    asmTryCatchBlocks.add(new AsmTryCatchBlock(start, end, handler, type));
                }


                @Override
                public void visitEnd() {
                    for (AsmTryCatchBlock tcb : asmTryCatchBlocks) {
                        super.visitTryCatchBlock(tcb.start, tcb.end, tcb.handler, tcb.type);
                    }
                    super.visitLocalVariable("t", ASM_TYPE_THROWABLE.getDescriptor(), null, startCatchBlock, endCatchBlock, newlocal);
                    super.visitEnd();
                }

            };
        }
    }

    @Override
    public void visitEnd() {
        //add wrapper native method
        if (this.addMethodNodes.size() != 0) {
            for (Method method : this.addMethodNodes) {
                boolean staticMethod = (Opcodes.ACC_STATIC & method.access) != 0;
                int newAccess = (Opcodes.ACC_PRIVATE | Opcodes.ACC_NATIVE | Opcodes.ACC_FINAL);
                newAccess = staticMethod ? newAccess | Opcodes.ACC_STATIC : newAccess;
                MethodVisitor mv = cv.visitMethod(newAccess, method.getName(), method.getDescriptor(), null, null);
                mv.visitEnd();
            }
        }
        super.visitEnd();
    }
}


/**
 * 用于Call的代码锁
 */
class CallAsmCodeLock extends AsmCodeLock {

    CallAsmCodeLock(AdviceAdapter aa) {
        super(
                aa,
                new int[]{
                        ICONST_2, POP
                },
                new int[]{
                        ICONST_3, POP
                }
        );
    }
}

/**
 * TryCatch块,用于ExceptionsTable重排序
 */
class AsmTryCatchBlock {

    final Label start;
    final Label end;
    final Label handler;
    final String type;

    AsmTryCatchBlock(Label start, Label end, Label handler, String type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

}
