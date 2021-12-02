package com.jrasp.system.module;

import com.jrasp.api.*;
import com.jrasp.api.Module;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@MetaInfServices(Module.class)
@Information(id = "heartbeat", author = "jrasp", version = "0.0.1")
public class HeartbeatModule extends ModuleLifecycleAdapter implements Module {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // 5 分钟一次心跳
    private static final long FREQUENCY = 5 * 60;

    private static final String pid;

    static {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        pid = name.split("@")[0];
    }

    private static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
            new BasicThreadFactory.Builder().namingPattern("jrasp-heartbeat-pool-%d").daemon(true).build());

    @Resource
    private ConfigInfo configInfo;

    private AtomicBoolean initialize = new AtomicBoolean(false);

    @Override
    public void loadCompleted() {
        start(); // 模块加载完成之后
    }

    @Override
    public void onUnload() throws Throwable {
        stop();  // 模块卸载时
    }

    private synchronized void start() {
        if (initialize.compareAndSet(false, true)) {
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        work();
                    } catch (Exception e) {
                        logger.error("error occurred when report heartbeat", e);
                    }
                }
            }, 1, FREQUENCY, TimeUnit.SECONDS); // to avoid dead lock, init time could not be 0
        }
    }

    // 心跳消息写入本地
    private void work() {
        logger.info("time: {}, ip: {}, pid: {}, port: {}, version: {}",
                new Date(), configInfo.getServerAddress().getHostName(), pid, configInfo.getServerAddress().getPort(), configInfo.getVersion());
    }

    private void stop() {
        if (initialize.compareAndSet(true, false)) {
            executorService.shutdown();
        }
    }
}
