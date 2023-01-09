package com.jrasp.agent.core.manager;

import com.jrasp.agent.core.util.SandboxProtector;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jrasp 逻辑优化
 * @author jvm-sandbox
 */
public class DefaultCoreLoadedClassDataSource {

    private final Instrumentation inst;

    public DefaultCoreLoadedClassDataSource(final Instrumentation inst) {
        this.inst = inst;
    }

    /**
     * @param classNameSets 类名称是 java.lang.String 而不是 java/lang/String
     * @return
     */
    public List<Class<?>> findTargetClass(Set<String> classNameSets) {
        SandboxProtector.instance.enterProtecting();
        try {
            final List<Class<?>> classes = new ArrayList<Class<?>>();
            if (null == classNameSets) {
                return classes;
            }
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                // clazz.getName() "java.lang.String"
                // TODO jvm名称、asm 名称和匹配名称 一致
                if (clazz != null && classNameSets.contains(clazz.getName())) {
                    classes.add(clazz);
                }
            }
            return classes;
        } finally {
            SandboxProtector.instance.exitProtecting();
        }
    }
}
