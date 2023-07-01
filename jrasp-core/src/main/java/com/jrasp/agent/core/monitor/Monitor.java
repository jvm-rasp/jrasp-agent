package com.jrasp.agent.core.monitor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jrasp
 * 代码来源于apm开源框架cat、jvm诊断工具
 * https://blog.csdn.net/m386084855/article/details/121929513
 */
public interface Monitor {
    /**
     * @return 监控名称
     */
    String getName();

    boolean isEnable();

    /**
     * @return 指标
     */
    Map<String, Object> getInfo();

    class Factory {

        public static Map<String, Monitor> maps = new ConcurrentHashMap<String, Monitor>(16);

        public static void init() {
            register(new JavaMemMonitor());
            register(new ProcessMonitor());
            register(new ThreadInfoMonitor());
        }

        public static LinkedHashMap<String, Object> collector() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(64);
            for (Monitor monitor : maps.values()) {
                if (monitor != null && monitor.isEnable()) {
                    Map<String, Object> info = monitor.getInfo();
                    if (info != null) {
                        result.putAll(info);
                    }
                }
            }
            return result;
        }

        public static void register(Monitor monitor) {
            if (monitor != null) {
                maps.put(monitor.getName(), monitor);
            }
        }

        public static void unRegister(Monitor monitor) {
            if (monitor != null) {
                maps.remove(monitor.getName());
            }
        }

        public static void clear() {
            if (maps != null) {
                maps.clear();
            }
        }
    }
}
