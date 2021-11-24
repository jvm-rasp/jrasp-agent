package com.jrasp.core.enhance.weaver;

public interface CodeLock {

    /**
     * 根据字节码流锁或解锁代码
     * 通过对字节码流的判断，决定当前代码是锁定和解锁
     *
     * @param opcode 字节码
     */
    void code(int opcode);

    /**
     * 判断当前代码是否还在锁定中
     *
     * @return true/false
     */
    boolean isLock();

    /**
     * 将一个代码块纳入代码锁保护范围
     *
     * @param block 代码块
     */
    void lock(Block block);

    /**
     * 代码块
     */
    interface Block {
        /**
         * 代码
         */
        void code();
    }

}