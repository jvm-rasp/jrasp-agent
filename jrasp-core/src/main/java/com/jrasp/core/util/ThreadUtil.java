package com.jrasp.core.util;

import java.util.*;

public class ThreadUtil {

    /**
     * 获取所有线程List
     *
     * @return
     */
    public static List<Thread> getThreadList() {
        List<Thread> result = new ArrayList<Thread>();
        ThreadGroup root = getRoot();
        Thread[] threads = new Thread[root.activeCount()];
        while (root.enumerate(threads, true) == threads.length) {
            threads = new Thread[threads.length * 2];
        }
        for (Thread thread : threads) {
            if (thread != null) {
                result.add(thread);
            }
        }
        return result;
    }

    private static ThreadGroup getRoot() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = group.getParent()) != null) {
            group = parent;
        }
        return group;
    }
}
