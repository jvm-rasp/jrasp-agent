package com.jrasp.agent.core.newlog;

public class LogRecord {
    private String topic;
    private Level level;
    private int logId;
    private String ts;
    private String processId;
    private String thread;
    private int pid;
    private String hostName;
    private String msg;
    private String stackTrace;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public LogRecord() {
    }

    public LogRecord(String message) {
        this.level = Level.INFO;
        this.msg = message;
    }

    public LogRecord(String topic, Level level, int logId, String ts, String processId, String thread,
                     int pid, String hostName, String msg) {
        this.topic = topic;
        this.level = level;
        this.logId = logId;
        this.ts = ts;
        this.processId = processId;
        this.thread = thread;
        this.pid = pid;
        this.hostName = hostName;
        this.msg = msg;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"msg\":\"").append(msg).append('\"');
        sb.append(",\"hostName\":\"").append(hostName).append('\"');
        sb.append(",\"pid\":").append(pid);
        sb.append(",\"thread\":\"").append(thread).append('\"');
        sb.append(",\"processId\":\"").append(processId).append('\"');
        sb.append(",\"ts\":\"").append(ts).append('\"');
        sb.append(",\"logId\":").append(logId);
        sb.append(",\"level\":\"").append(level.toString()).append('\"');
        sb.append(",\"topic\":\"").append(topic).append('\"');
        sb.append(",\"stackTrace\":\"").append(stackTrace).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
