package com.jrasp.agent.api.matcher;

import com.jrasp.agent.api.listener.AdviceListener;

/**
 * @author jrasp
 * 以 java.io.FileInputStream 参数为File构造器为例子
 * className: java/io/FileInputStream
 * methodName:<init>
 * methodDesc：(Ljava/io/File;)V
 */
public class MethodMatcher {

    private String className;

    private String sign;

    private AdviceListener adviceListener;

    // 是否hook
    private boolean isHook = false;

    public MethodMatcher(String sign, AdviceListener adviceListener) {
        this.sign = sign;
        this.adviceListener = adviceListener;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }


    public AdviceListener getAdviceListener() {
        return adviceListener;
    }

    public void setAdviceListener(AdviceListener adviceListener) {
        this.adviceListener = adviceListener;
    }

    public boolean isHook() {
        return isHook;
    }

    public void setHook(boolean hook) {
        isHook = hook;
    }

    public String desc() {
        return className + "#" + sign;
    }

    @Override
    public String toString() {
        return className + "#" + sign;
    }
}
