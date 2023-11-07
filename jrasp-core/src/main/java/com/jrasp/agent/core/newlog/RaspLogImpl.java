package com.jrasp.agent.core.newlog;

import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.core.json.JSONObject;

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

    private String processId;

    public RaspLogImpl(BlockingQueue<String> queue, String processId) {
        this.queue = queue;
        this.processId = processId;
    }

    /**
     * 记录攻击日志
     *
     * @param attackInfo
     */
    @Override
    public void attack(AttackInfo attackInfo) {
        Context context = attackInfo.getContext();
        JSONObject contextObject = new JSONObject();
        contextObject.put("method", context.getMethod());
        contextObject.put("protocol", context.getProtocol());
        contextObject.put("localAddr", context.getLocalAddr());
        contextObject.put("remoteHost", context.getRemoteHost());
        contextObject.put("requestURL", context.getRequestURL());
        contextObject.put("requestURI", context.getRequestURI());
        contextObject.put("contentType", context.getContentType());
        contextObject.put("contentLength", context.getContentLength());
        contextObject.put("characterEncoding", context.getCharacterEncoding());
        contextObject.put("header", context.getHeader());
        contextObject.put("queryString", context.getQueryString());
        contextObject.put("marks", context.getMarks());
        contextObject.put("body", context.getBody());
        String contextString = contextObject.toString();
        JSONObject object = new JSONObject();
        object.put("context", contextString);
        object.put("metaInfo", attackInfo.getMetaInfo());
        object.put("stackTrace", attackInfo.getStackTrace());
        object.put("payload", attackInfo.getPayload());
        object.put("isBlocked", attackInfo.isBlocked());
        object.put("attackType", attackInfo.getAttackType());
        object.put("attackTime", System.currentTimeMillis());
        object.put("algorithm", attackInfo.getAlgorithm());
        object.put("extend", attackInfo.getExtend());
        object.put("level", attackInfo.getLevel());
        publish(Level.ATTACK, DEFAULT_LOG_ID, object.toString());
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
        String msg = LogFormatter.format(level, logId, message, processId, t);
        /*
         * put: 队列满，阻塞当前线程
         * offer:队列满，返回false
         */
        return queue.offer(msg);
    }
}
