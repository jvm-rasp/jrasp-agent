package com.jrasp.agent.core.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jrasp
 * 进程负载
 */
public class ProcessMonitor implements Monitor {

    /**
     * @return 监控名称
     */
    @Override
    public String getName() {
        return "system.process";
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
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        collectOperatingSystemData(map);
        return map;
    }

    private void collectOperatingSystemData(Map<String, Object> map) {
        OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();

        map.put("system.load.average", operatingSystem.getSystemLoadAverage());

        if (operatingSystem instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) operatingSystem;
            map.put("cpu.system.load.percent", osBean.getSystemCpuLoad() * 100);
            map.put("cpu.jvm.load.percent", osBean.getProcessCpuLoad() * 100);
        }
    }

}
