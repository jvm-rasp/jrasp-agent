package com.jrasp.module.admin;

import com.jrasp.api.*;
import com.jrasp.api.Module;
import com.jrasp.api.log.Log;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.kohsuke.MetaInfServices;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jrasp.module.admin.AdminModuleLogIdConstant.*;

@MetaInfServices(Module.class)
@Information(id = "heartbeat", author = "jrasp", version = "0.0.1")
public class HeartbeatModule extends ModuleLifecycleAdapter implements Module {

    @Resource
    private Log logger;

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
                        logger.error(HEART_BEAT_MODULE_WORK_ERROR_LOG_ID,"error occurred when report heartbeat", e);
                    }
                }
            }, 1, FREQUENCY, TimeUnit.SECONDS); // to avoid dead lock, init time could not be 0
        }
    }

    // 心跳消息写入本地
    private void work() {
        logger.info(HEART_BEAT_MODULE_WORK_LOG_ID,"ip: {}, pid: {}, port: {}, version: {}", configInfo.getServerAddress().getHostName(), pid, configInfo.getServerAddress().getPort(), configInfo.getVersion());
    }

    private void stop() {
        if (initialize.compareAndSet(true, false)) {
            executorService.shutdown();
        }
    }
}
