package com.jrasp.agent.core.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jrasp
 * jvm 堆空间、元空间/永久代使用占比
 */
public class JavaMemMonitor implements Monitor {
    /**
     * @return 监控名称
     */
    @Override
    public String getName() {
        return "jvm.memory";
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    /**
     * @return 指标
     */
    @Override
    public Map<String, Object> getInfo() {
        return doMemoryMonitor();
    }

    public Map<String, Object> doMemoryMonitor() {

        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();

        long usedPermGen = 0L;
        long maxPermGen = 0L;
        final MemoryPoolMXBean permGenMemoryPool = getPermGenMemoryPool();
        if (permGenMemoryPool != null) {
            final MemoryUsage usage = permGenMemoryPool.getUsage();
            usedPermGen = usage.getUsed();
            maxPermGen = usage.getMax();
        }

        long usedMetaSpace = 0L;
        long maxMetaSpace = 0L;

        final MemoryPoolMXBean metaSpaceMemoryPool = getMetaspaceMemoryPool();
        if (metaSpaceMemoryPool != null) {
            final MemoryUsage usage = metaSpaceMemoryPool.getUsage();
            usedMetaSpace = usage.getUsed();
            maxMetaSpace = usage.getMax();
        }

        Map<String, Object> map = new LinkedHashMap<String, Object>();

        map.put("jvm.memory.used", usedMemory);
        map.put("jvm.memory.used.percent", 100d * usedMemory / maxMemory); // 关键监控指标
        map.put("jvm.memory.perm.used", usedPermGen);
        map.put("jvm.memory.perm.used.percent", usedPermGen > 0 && maxPermGen > 0 ? 100d * usedPermGen / maxPermGen : -1d);
        map.put("jvm.memory.metaspace.used", usedMetaSpace);
        map.put("jvm.memory.metaspace.used.percent", usedMetaSpace > 0 && maxMetaSpace > 0 ? 100d * usedMetaSpace / maxMetaSpace : -1d);

        return map;
    }

    private MemoryPoolMXBean getMetaspaceMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool != null && memoryPool.getName().endsWith("Metaspace")) {
                return memoryPool;
            }
        }
        return null;
    }

    private MemoryPoolMXBean getPermGenMemoryPool() {
        for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPool != null && memoryPool.getName().endsWith("Perm Gen")) {
                return memoryPool;
            }
        }
        return null;
    }


}
