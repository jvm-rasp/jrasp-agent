package com.jrasp.agent.core.newlog;

import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;

import java.util.concurrent.BlockingQueue;

/**
 * 日志写入队列
 *
 * @author jrasp
 * @since 2023/5/27
 */
public class RaspLogImpl implements RaspLog {

    private static final int DEFAULT_LOG_ID = 0;

    private BlockingQueue<String> queue = null;

    public RaspLogImpl(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    /**
     * 记录攻击日志
     *
     * @param attackInfo
     */
    @Override
    public void attack(AttackInfo attackInfo) {
        publish(Level.ATTACK, DEFAULT_LOG_ID, attackInfo.toJSON());
    }

    @Override
    public void info(String message) {
        publish(Level.INFO, DEFAULT_LOG_ID, message);
    }

    @Override
    public void warning(String message) {
        publish(Level.WARNING, DEFAULT_LOG_ID, message);
    }

    @Override
    public void error(String message) {
        publish(Level.ERROR, DEFAULT_LOG_ID, message);
    }

    @Override
    public void error(String message, Throwable t) {
        publish(Level.ERROR, DEFAULT_LOG_ID, message, t);
    }

    //---------------------新版本日志------------------------

    @Override
    public void info(int logId, String message) {
        publish(Level.INFO, logId, message);
    }

    @Override
    public void warning(int logId, String message) {
        publish(Level.WARNING, logId, message);
    }

    @Override
    public void warning(String message, Throwable t) {
        publish(Level.WARNING, DEFAULT_LOG_ID, message, t);
    }

    @Override
    public void error(int logId, String message) {
        publish(Level.ERROR, logId, message);
    }

    @Override
    public void warning(int logId, String message, Throwable t) {
        publish(Level.WARNING, logId, message, t);
    }

    @Override
    public void error(int logId, String message, Throwable t) {
        publish(Level.ERROR, logId, message, t);
    }

    private boolean publish(Level level, int logId, String message) {
        return publish(level, logId, message, null);
    }

    private boolean publish(Level level, int logId, String message, Throwable t) {
        String msg = LogFormatter.format(level, logId, message, t);
        /*
         * put()方法是如果队列如果满，阻塞当前线程挂起
         * offer()方法是队列满的话就会返回false
         */
        return queue.offer(msg);
    }
}