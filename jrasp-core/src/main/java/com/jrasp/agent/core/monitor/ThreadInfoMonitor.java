package com.jrasp.agent.core.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author jrasp
 * 线程状态监控
 */
public class ThreadInfoMonitor implements Monitor {

    @Override
    public String getName() {
        return "jvm.thread";
    }

    @Override
    public Map<String, Object> getInfo() {
        return doThreadCollect();
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    private Map<String, Object> doThreadCollect() {
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("jvm.thread.count", threadBean.getThreadCount());
        map.put("jvm.thread.daemon.count", threadBean.getDaemonThreadCount());
        map.put("jvm.thread.totalstarted.count", threadBean.getTotalStartedThreadCount());

        int newThreadCount = 0;
        int runnableThreadCount = 0;
        int blockedThreadCount = 0;
        int waitThreadCount = 0;
        int timeWaitThreadCount = 0;
        int terminatedThreadCount = 0;

        ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadBean.getAllThreadIds());
        if (threadInfos != null) {
            for (ThreadInfo threadInfo : threadInfos) {
                if (threadInfo != null) {
                    switch (threadInfo.getThreadState()) {
                        case NEW:
                            newThreadCount++;
                            break;
                        case RUNNABLE:
                            runnableThreadCount++;
                            break;
                        case BLOCKED:
                            blockedThreadCount++;
                            break;
                        case WAITING:
                            waitThreadCount++;
                            break;
                        case TIMED_WAITING:
                            timeWaitThreadCount++;
                            break;
                        case TERMINATED:
                            terminatedThreadCount++;
                            break;
                        default:
                            break;
                    }
                } else {
                    terminatedThreadCount++;
                }
            }
        }

        map.put("jvm.thread.new.count", newThreadCount);
        map.put("jvm.thread.runnable.count", runnableThreadCount);
        // 重点关注的指标
        // TODO 线程block由rasp引起
        map.put("jvm.thread.blocked.count", blockedThreadCount);
        map.put("jvm.thread.waiting.count", waitThreadCount);
        map.put("jvm.thread.time_waiting.count", timeWaitThreadCount);
        map.put("jvm.thread.terminated.count", terminatedThreadCount);

        long[] ids = threadBean.findDeadlockedThreads();

        // 重点关注的指标
        map.put("jvm.thread.deadlock.count", ids == null ? 0 : ids.length);

        // TODO 死锁由rasp引起

        if (threadInfos != null) {
            int tomcatThreadsCount = countThreadsByPrefix(threadInfos, "http-", "catalina-exec-");
            int jettyThreadsCount = countThreadsBySubstring(threadInfos, "@qtp");
            map.put("jvm.thread.http.count", tomcatThreadsCount + jettyThreadsCount);
        }

        return map;
    }

    private int countThreadsByPrefix(ThreadInfo[] threads, String... prefixes) {
        int count = 0;

        for (ThreadInfo thread : threads) {
            if (thread != null) {
                for (String prefix : prefixes) {
                    if (String.valueOf(thread.getThreadName()).startsWith(prefix)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private int countThreadsBySubstring(ThreadInfo[] threads, String... substrings) {
        int count = 0;

        for (ThreadInfo thread : threads) {
            if (thread != null) {
                for (String str : substrings) {
                    if (String.valueOf(thread.getThreadName()).contains(str)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
