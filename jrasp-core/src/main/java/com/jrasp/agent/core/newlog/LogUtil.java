package com.jrasp.agent.core.newlog;

import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.core.CoreConfigure;
import com.jrasp.agent.core.newlog.consumer.ConsoleConsumer;
import com.jrasp.agent.core.newlog.consumer.FileConsumer;
import com.jrasp.agent.core.newlog.consumer.LogConsumer;
import com.jrasp.agent.core.newlog.consumer.RemoteConsumer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * 日志的操作入口
 *
 * @author jrasp
 */
public class LogUtil {

    private static volatile RaspLogImpl raspLog = null;

    private static FileConsumer fileConsumer = null;

    private static ConsoleConsumer consoleConsumer = null;

    private static RemoteConsumer remoteConsumer = null;

    // 多个消费者
    private static volatile LinkedList<LogConsumer> logConsumers = new LinkedList<LogConsumer>();

    public static synchronized void init(BlockingQueue<String> queue, String logPath, String processId) {
        raspLog = new RaspLogImpl(queue, processId);
        fileConsumer = new FileConsumer(logPath);
        consoleConsumer = new ConsoleConsumer();
        // file优先级高于console
        logConsumers.add(fileConsumer);
        logConsumers.add(consoleConsumer);
    }

    public static synchronized void initRemoteConsumer(RemoteConsumer logConsumer) {
        remoteConsumer = logConsumer;
        logConsumers.addFirst(logConsumer);
    }

    public static synchronized LogConsumer getAvailableConsumer() {
        for (int i = 0; i < logConsumers.size(); i++) {
            LogConsumer logConsumer = logConsumers.get(i);
            //　TODO 这里检测是否恢复
            if (logConsumer != null && logConsumer.isAvailable()) {
                return logConsumer;
            }
        }
        // 至少可以获取 console consumer
        return null;
    }

    public static RaspLog getLogger() {
        return raspLog;
    }

    public static synchronized void close() {
        if (logConsumers != null) {
            for (LogConsumer logConsumer : logConsumers) {
                logConsumer.close();
            }
            logConsumers.clear();
            logConsumers = null;
        }
        raspLog = null;
    }

    public static void syncFileLog() throws Exception {
        if (fileConsumer != null && fileConsumer.isAvailable()) {
            List<String> logs = fileConsumer.syncOfflineLogs();
            if (remoteConsumer != null && remoteConsumer.isAvailable()) {
                for (String log : logs) {
                    remoteConsumer.consumer(log);
                }
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 记录攻击日志
     *
     * @param attackInfo
     */
    public static void attack(AttackInfo attackInfo) {
        if (raspLog != null) {
            raspLog.attack(attackInfo);
        }
    }

    public static void info(String message) {
        if (raspLog != null) {
            raspLog.info(message);
        }
    }

    public static void warning(String message) {
        if (raspLog != null) {
            raspLog.warning(message);
        }
    }

    public static void warning(String message, Throwable t) {
        if (raspLog != null) {
            raspLog.warning(message, t);
        }
    }

    public static void error(String message) {
        if (raspLog != null) {
            raspLog.error(message);
        }
    }

    public static void error(String message, Throwable t) {
        if (raspLog != null) {
            raspLog.error(message, t);
        }
    }

    public static void info(int logId, String message) {
        if (raspLog != null) {
            raspLog.info(logId, message);
        }
    }

    public static void warning(int logId, String message) {
        if (raspLog != null) {
            raspLog.warning(logId, message);
        }
    }

    public static void error(int logId, String message) {
        if (raspLog != null) {
            raspLog.error(logId, message);
        }
    }

    public static void warning(int logId, String message, Throwable t) {
        if (raspLog != null) {
            raspLog.warning(logId, message, t);
        }
    }

    public static void error(int logId, String message, Throwable t) {
        if (raspLog != null) {
            raspLog.error(logId, message, t);
        }
    }

}
