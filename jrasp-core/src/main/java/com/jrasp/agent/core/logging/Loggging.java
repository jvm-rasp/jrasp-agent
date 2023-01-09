package com.jrasp.agent.core.logging;

import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 * Log日志框架工具类
 *
 * @author jrasp
 * 由于log4j、logback的严重安全漏洞，使用jdk自带的日志框架
 */
public class Loggging implements RaspLog {

    public static Loggging INSTANCE = new Loggging();

    // 一个进程一个文件
    private static final int fileCount = 1;

    // 滚动 100M
    private static final int LogLimit = 100 * 1024 * 1024;

    private final static String AGENT_LOG_FILE_NAME = "jrasp-agent-%u.log";

    private final static String MODULE_LOG_FILE_NAME = "jrasp-module-%u.log";

    private final static String ATTACK_LOG_FILE_NAME = "jrasp-attack-%u.log";

    private static Logger AGENT_LOG = Logger.getLogger("com.jrasp.agent.core");

    private static Logger MODULE_LOG = Logger.getLogger("com.jrasp.agent.module");

    private static Logger ATTACK_LOG = Logger.getLogger("attack");

    private static FileHandler agentLogHandler;

    private static FileHandler moduleLogHandler;

    private static FileHandler attackLogHandler;

    /**
     * 初始化日志框架
     * TODO 不能修改业务配置 LogManager
     */
    public static void init(String logDir) throws IOException {
        uninstallSLF4JBridgeHandler();

        // agent 日志
        agentLogHandler = new FileHandler(logDir + File.separator + AGENT_LOG_FILE_NAME, LogLimit, fileCount, true);
        agentLogHandler.setFormatter(new LogFormatter());
        AGENT_LOG.addHandler(agentLogHandler);
        AGENT_LOG.setLevel(Level.INFO);
        // module 日志
        moduleLogHandler = new FileHandler(logDir + File.separator + MODULE_LOG_FILE_NAME, LogLimit, fileCount, true);
        moduleLogHandler.setFormatter(new LogFormatter());
        MODULE_LOG.addHandler(moduleLogHandler);
        MODULE_LOG.setLevel(Level.INFO);
        // 攻击日志
        attackLogHandler = new FileHandler(logDir + File.separator + ATTACK_LOG_FILE_NAME, LogLimit, fileCount, true);
        attackLogHandler.setFormatter(new LogFormatter());
        ATTACK_LOG.addHandler(attackLogHandler);
        ATTACK_LOG.setLevel(Level.INFO);
    }

    /**
     * 销毁日志框架
     *
     * @see java.util.logging.Logger#handlers 持有 handler 必须清除，否则内存泄漏
     */
    public static void destroy() {
        if (agentLogHandler != null) {
            AGENT_LOG.removeHandler(agentLogHandler);
            agentLogHandler.close();
            AGENT_LOG = null;
        }
        if (moduleLogHandler != null) {
            MODULE_LOG.removeHandler(moduleLogHandler);

            moduleLogHandler.close();
            MODULE_LOG = null;
        }
        if (attackLogHandler != null) {
            ATTACK_LOG.removeHandler(attackLogHandler);
            attackLogHandler.close();
            ATTACK_LOG = null;
        }
    }

    /**
     * remove jul-to-sl4j2
     * springboot 自带 jul-to-slf4j
     *
     * @throws SecurityException
     */
    public static void uninstallSLF4JBridgeHandler() throws SecurityException {
        java.util.logging.Logger rootLogger = getRootLogger();
        Handler[] handlers = rootLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].getClass().getName().endsWith("SLF4JBridgeHandler")) {
                rootLogger.removeHandler(handlers[i]);
            }
        }
    }

    private static java.util.logging.Logger getRootLogger() {
        return LogManager.getLogManager().getLogger("");
    }

    //-------------------------------------------- inject interface

    @Override
    public void attack(AttackInfo attackInfo) {
        ATTACK_LOG.warning(attackInfo.toJSON());
    }

    @Override
    public void info(String message) {
        MODULE_LOG.info(message);
    }

    @Override
    public void warning(String message) {
        MODULE_LOG.warning(message);
    }

    @Override
    public void error(String message) {
        MODULE_LOG.severe(message);
    }

    @Override
    public void error(String message, Throwable thrown) {
        MODULE_LOG.log(Level.SEVERE, message, thrown);
    }
}
