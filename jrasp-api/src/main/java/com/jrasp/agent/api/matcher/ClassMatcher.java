package com.jrasp.agent.api.matcher;

import com.jrasp.agent.api.listener.AdviceListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类匹配
 * 一个类对应一个class
 * 一个class下有一个或者多个hook方法，每个方法hook对应一个监听器listener
 *
 * @author jrasp
 */
public class ClassMatcher {

    private boolean forceLoad = false;

    private final String className;

    private final String classNameJdk;

    private Map<String/*name+desc*/, MethodMatcher> methodMatcherMap = new ConcurrentHashMap<String, MethodMatcher>(16);

    public MethodMatcher findMethodMatcher(String signature) {
        return methodMatcherMap.get(signature);
    }

    public ClassMatcher(String className) {
        this.className = className;
        this.classNameJdk = className.replace("/", ".");
    }

    public ClassMatcher(String className, boolean forceLoad) {
        this.className = className;
        this.forceLoad = forceLoad;
        this.classNameJdk = className.replace("/", ".");
    }

    // 简单匹配模式
    public ClassMatcher onMethod(MethodMatcher methodMatcher) {
        methodMatcher.setClassName(classNameJdk);
        methodMatcherMap.put(methodMatcher.getSign(), methodMatcher);
        return this;
    }

    // 多选模式
    public ClassMatcher onMethod(MethodMatcher[] methodMatchers, AdviceListener adviceListener) {
        for (MethodMatcher methodMatcher : methodMatchers) {
            methodMatcher.setAdviceListener(adviceListener);
            methodMatcher.setClassName(classNameJdk);
            methodMatcherMap.put(methodMatcher.getSign(), methodMatcher);
        }
        return this;
    }

    public ClassMatcher onMethod(String sign, AdviceListener adviceListener) {
        MethodMatcher m = new MethodMatcher(sign, adviceListener);
        m.setClassName(classNameJdk);
        methodMatcherMap.put(sign, m);
        return this;
    }

    public ClassMatcher onMethod(String[] signs, AdviceListener adviceListener) {
        for (String sign : signs) {
            MethodMatcher m = new MethodMatcher(sign, adviceListener);
            m.setClassName(classNameJdk);
            methodMatcherMap.put(sign, m);
        }
        return this;
    }

    public String getClassName() {
        return className;
    }

    public String getClassNameJdk() {
        return classNameJdk;
    }

    public boolean isForceLoad() {
        return forceLoad;
    }

    public void setForceLoad(boolean forceLoad) {
        this.forceLoad = forceLoad;
    }

    public Map<String, MethodMatcher> getMethodMatcherMap() {
        return methodMatcherMap;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry<String, MethodMatcher> entry : methodMatcherMap.entrySet()) {
            stringBuffer.append(className + "#" + entry.getKey() + "," + entry.getValue().isHook() + "\n");
        }
        return stringBuffer.toString();
    }

    /**
     * classname 作为唯一条件
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassMatcher that = (ClassMatcher) o;

        return className != null ? className.equals(that.className) : that.className == null;
    }

    @Override
    public int hashCode() {
        return className != null ? className.hashCode() : 0;
    }
}
