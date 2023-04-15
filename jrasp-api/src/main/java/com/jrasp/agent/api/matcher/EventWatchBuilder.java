package com.jrasp.agent.api.matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author jrasp
 */
public class EventWatchBuilder {

    private final Map<String, ClassMatcher> classMatchers = new HashMap<String, ClassMatcher>();

    private final ModuleEventWatcher moduleEventWatcher;

    public EventWatchBuilder(ModuleEventWatcher moduleEventWatcher) {
        this.moduleEventWatcher = moduleEventWatcher;
    }

    public EventWatchBuilder onClass(ClassMatcher classMatcher) {
        // bugfix: 用一个类的不同方法分布在不同的ClassMatcher中
        if (classMatcher != null) {
            String className = classMatcher.getClassName();
            ClassMatcher classMatcherCache = classMatchers.get(className);
            if (classMatcherCache != null) {
                Map<String, MethodMatcher> methodMatcherMapCache = classMatcherCache.getMethodMatcherMap();
                classMatcher.getMethodMatcherMap().putAll(methodMatcherMapCache);
            }
            classMatchers.put(classMatcher.getClassName(), classMatcher);
        }
        return this;
    }

    /**
     * TODO 需要优化下，防止用户出现忘记调用build的情况
     */
    public void build() {
        if (moduleEventWatcher != null) {
            moduleEventWatcher.watch(new HashSet<ClassMatcher>(classMatchers.values()));
        }
    }
}
