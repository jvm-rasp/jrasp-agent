package com.jrasp.agent.core.task;

import com.jrasp.agent.core.monitor.Monitor;
import com.jrasp.agent.core.newlog.LogUtil;

import java.util.Iterator;
import java.util.Map;

/**
 * @author jrasp
 * 2023-0623
 */
public class HeartbeatTask extends AbstractRaspTask {

    @Override
    public void run() {
        String result = work();
        LogUtil.info(result.toString());
    }

    /**
     * 心跳消息
     */
    private static String work() {
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
        return sb.toString();
    }

    private static void appendJsonField(StringBuffer sb, String key, String value, boolean hasNext) {
        sb.append("\"").append(key).append("\": ");
        sb.append("\"").append(value.replace("\"", "\\\"")).append("\"");
        if (hasNext) {
            sb.append(", ");
        }
    }

    public static void main(String[] args) {
        work();
    }

}
