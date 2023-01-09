package com.jrasp.agent.api.util;

/**
 * 调用栈
 *
 * @author jrasp
 */
public class StackTrace {

    /**
     * RASP自身的栈开始位置
     *
     * @see java.com.jrasp.agent.bridge110.Spy
     */
    private final static String JRASP_STACK_END = "java.com.jrasp.agent.bridge";


    public static String[] getStackTraceString() {
        return getStackTraceString(100,true);
    }
    /**
     * @param maxStack 输出的最大栈深度
     * @return
     */
    public static String[] getStackTraceString(int maxStack, boolean hasLineNumber) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        // 找到用户栈开始index
        int i;
        for (i = 0; i < stackTraceElements.length; i++) {
            String className = stackTraceElements[i].getClassName();
            if (className != null && className.startsWith(JRASP_STACK_END)) {
                break;
            }
        }
        int endIndex = Math.min(i + maxStack, stackTraceElements.length - 1);
        String[] effectiveArray = new String[endIndex - i];
        // 获取有用的栈
        for (int k = i + 1; k <= endIndex; k++) {
            String info = "";
            StackTraceElement tmp = stackTraceElements[k];
            if (hasLineNumber) {
                // 不包含行号
                info = tmp.toString();
            } else {
                info = tmp.getClassName() + "." + tmp.getMethodName();
            }
            effectiveArray[k - i - 1] = info;
        }
        return effectiveArray;
    }

    /**
     * @param maxStack 输出的最大栈深度
     * @return
     */
    public static StackTraceElement[] getStackTraceObject(int maxStack) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        // 找到用户栈开始index
        int i;
        for (i = 0; i < stackTraceElements.length; i++) {
            String className = stackTraceElements[i].getClassName();
            if (className != null && className.startsWith(JRASP_STACK_END)) {
                break;
            }
        }
        int endIndex = Math.min(i + maxStack, stackTraceElements.length - 1);
        // 使用arrasy.copy
        StackTraceElement[] effectiveArray = new StackTraceElement[endIndex - i];
        // 获取有用的栈
        for (int k = i + 1; k <= endIndex; k++) {
            effectiveArray[k - i - 1] = stackTraceElements[k];
        }
        return effectiveArray;
    }



}
