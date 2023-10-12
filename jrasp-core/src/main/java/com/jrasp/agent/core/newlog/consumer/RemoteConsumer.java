package com.jrasp.agent.core.newlog.consumer;

import com.jrasp.agent.core.client.socket.RaspSocket;

/**
 *  @author jrasp
 */
public class RemoteConsumer extends AbstractConsumer {

    private RaspSocket raspSocket;

    public RemoteConsumer(RaspSocket raspSocket) {
        this.raspSocket = raspSocket;
    }

    /**
     * 消费队列，写入文件或者日志
     *
     * @param msg
     */
    @Override
    public void consumer(String msg) throws Exception {
        if (raspSocket != null) {
            raspSocket.write(msg);
        }
    }

    @Override
    public void close() {
        if (raspSocket != null && !raspSocket.isClosed()) {
            try {
                raspSocket.close();
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
            }
        }
    }

    public boolean isAvailable() {
        return enable && available && !raspSocket.isClosed();
    }

}
