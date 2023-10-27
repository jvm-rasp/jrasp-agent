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
        LogUtil.info(result);
    }

    /**
     * 心跳消息
     */
    private static String work() {
        Map<String, Object> collector = Monitor.Factory.collector();
        StringBuffer sb = new StringBuffer();
        Iterator<Map.Entry<String, Object>> iterator = collector.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            sb.append(next.getKey());
            sb.append("=");
            sb.append(next.getValue());
            sb.append(";");
        }
        return sb.toString();
    }

}
