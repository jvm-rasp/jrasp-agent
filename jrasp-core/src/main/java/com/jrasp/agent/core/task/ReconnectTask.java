package com.jrasp.agent.core.task;

import com.jrasp.agent.core.client.socket.RaspSocket;
import com.jrasp.agent.core.newlog.LogUtil;

/**
 * client socket 定时重连任务
 *
 * @author jrasp
 * @since 2023/05/27
 */
public class ReconnectTask extends AbstractRaspTask {

    private RaspSocket raspSocket;

    public ReconnectTask(RaspSocket raspSocket) {
        this.raspSocket = raspSocket;
    }

    @Override
    public void run() {
        if (raspSocket != null && !raspSocket.isClosed()) {
            return;
        }

        try {
            raspSocket.connect();
            LogUtil.info("rasp socket reconnect success.");
            LogUtil.syncFileLog();
        } catch (Exception e) {
            handleError(e);
        }
    }
}