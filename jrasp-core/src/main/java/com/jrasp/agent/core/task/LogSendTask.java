package com.jrasp.agent.core.task;

import com.jrasp.agent.core.newlog.LogUtil;
import com.jrasp.agent.core.newlog.consumer.LogConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 从缓存队列中取出日志，发送给daemon或者本地文件、kafka、终端等
 * @author jrasp
 */
public class LogSendTask extends AbstractRaspTask {

    /***
     * 批量操作的最大条数
     */
    private static final int MAX_BATCH_SIZE = 50;

    /**
     * 等待时长
     */
    private static final long MAX_WAIT_TIME = 500;

    private final BlockingQueue<String> queue;

    public LogSendTask(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        List<String> willSend = batchPoll();
        if (willSend == null || willSend.size() == 0) {
            return;
        }

        // 获取优先级最高的一个消费者
        LogConsumer logConsumer = LogUtil.getAvailableConsumer();
        if (logConsumer == null) {
            return;
        }

        for (String data : willSend) {
            if (data == null || "".equals(data)) {
                continue;
            }
            if (logConsumer.isAvailable()) {
                try {
                    logConsumer.consumer(data);
                } catch (Exception e) {
                    handlerException(logConsumer, data, e);
                }
            }
        }
    }

    // 异常消息处理
    private static void handlerException(LogConsumer currentLogConsumer, String data, Exception e) {
        // 关闭当前consumer
        if (currentLogConsumer.isAvailable()) {
            // 关闭 consumer
            currentLogConsumer.setAvailable(false);
        }
        LogUtil.error("consumer message error", e);
        // 获取可用的consumer
        LogConsumer nextLogConsumer = LogUtil.getAvailableConsumer();
        if (nextLogConsumer == null) {
            return;
        }
        // 异常消息消费，避免丢数据
        try {
            nextLogConsumer.consumer(data);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<String> batchPoll() {
        List<String> willSend = null;
        try {
            willSend = new ArrayList<String>(20);
            String message;
            int i = 0;
            while (++i < MAX_BATCH_SIZE &&
                    (message = queue.poll(MAX_WAIT_TIME, TimeUnit.MILLISECONDS)) != null) {
                willSend.add(message);
            }
            return willSend;
        } catch (Exception e) {
            handleError(e);
        }
        return willSend;
    }
}
