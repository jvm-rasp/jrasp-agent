package com.jrasp.agent.core.newlog.consumer;

/**
 * 输出到终端
 * @author jrasp
 */
public class ConsoleConsumer extends AbstractConsumer {

    /**
     * ConsoleConsumer 默认关闭
     *
     * @return
     */
    @Override
    public boolean isEnable() {
        return true;
    }

    /**
     * 消费队列，写入文件、socket和console
     *
     * @param msg
     */
    @Override
    public void consumer(String msg) throws Exception {
        System.out.println(msg);
    }

}
