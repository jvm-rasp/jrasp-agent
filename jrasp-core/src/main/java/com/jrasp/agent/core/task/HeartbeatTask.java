package com.jrasp.agent.core.task;

import com.jrasp.agent.core.monitor.Monitor;
import com.jrasp.agent.core.util.ProcessHelper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jrasp
 */
public class HeartbeatTask {

    private static final Logger LOGGER = Logger.getLogger(HeartbeatTask.class.getName());

    private static AtomicBoolean initialize = new AtomicBoolean(false);

    // 5 分钟一次心跳
    private static final long FREQUENCY = 5 * 60;

    private static final String pid = ProcessHelper.getCurrentPID();

    private static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    public static synchronized void start() {
        if (initialize.compareAndSet(false, true)) {
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        work();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "error occurred when report heartbeat", e);
                    }
                }
            }, 1, FREQUENCY, TimeUnit.SECONDS); // to avoid dead lock, init time could not be 0
        }
    }

    /**
     * 心跳消息
     */
    private static void work() {
        Map<String, Object> collector = Monitor.Factory.collector();

        StringBuffer sb = new StringBuffer();
        sb.append("{");
        Iterator<Map.Entry<String, Object>> iterator = collector.entrySet().iterator();
        int cnt = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            appendJsonField(sb, next.getKey(), next.getValue().toString(), cnt < collector.size() - 1);
            cnt++;
        }
        sb.append("}");
        LOGGER.log(Level.INFO, "monitor: {0}", new Object[]{sb.toString()});
    }

    public static void stop() {
        if (initialize.compareAndSet(true, false)) {
            executorService.shutdown();
            LOGGER.log(Level.INFO, "java agent [{0}] heartheat task stop ", new Object[]{pid});
        }
    }

    private static void appendJsonField(StringBuffer sb, String key, String value, boolean hasNext) {
        sb.append("\"").append(key).append("\": ");
        sb.append("\"").append(value.replace("\"", "\\\"")).append("\"");
        if (hasNext) {
            sb.append(", ");
        }
    }
}
