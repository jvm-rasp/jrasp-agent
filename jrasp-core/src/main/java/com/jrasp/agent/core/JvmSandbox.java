package com.jrasp.agent.core;

import com.jrasp.agent.core.enhance.weaver.EventListenerHandler;
import com.jrasp.agent.core.enhance.weaver.asm.EventWeaver;
import com.jrasp.agent.core.manager.DefaultCoreLoadedClassDataSource;
import com.jrasp.agent.core.manager.DefaultCoreModuleManager;
import com.jrasp.agent.core.manager.RaspClassFileTransformer;
import com.jrasp.agent.core.util.RaspClassUtils;
import com.jrasp.agent.core.util.SpyUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 沙箱
 *
 * @author jrasp
 */
public class JvmSandbox {

    /**
     * 需要提前加载的sandbox工具类
     * 提前加载，避免多线程加载类导致的死锁问题
     */
    private final static String[] earlyLoadSandboxClassNameArrays = new String[]{"com.jrasp.agent.core.util.RaspClassUtils"};

    private final CoreConfigure cfg;
    private final Instrumentation inst;
    private final DefaultCoreModuleManager coreModuleManager;

    private Thread clearThread = new Thread(new Runnable() {
        @Override
        public void run() {
            FileUtils.deleteQuietly(new File(cfg.getProcessPidPath()));
        }
    }, "rasp-shutdown-hook");

    public JvmSandbox(final CoreConfigure cfg,
                      final Instrumentation inst) {
        EventListenerHandler.getSingleton();
        this.cfg = cfg;
        this.inst = inst;
        this.coreModuleManager = new DefaultCoreModuleManager(cfg, inst, new DefaultCoreLoadedClassDataSource(inst));
        init();
    }

    private void init() {
        // 输出技术支持链接，方便业务排查问题
        System.out.println(String.format("%s  INFO [jrasp] %s %s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss").format(new Date()),
                "开启RASP安全防护，技术支持:", CoreConfigure.JRASP_SUPPORT_URL));

        initPidRunDir();
        RaspClassUtils.doEarlyLoadSandboxClass(earlyLoadSandboxClassNameArrays);
        SpyUtils.init(cfg.getNamespace());
        inst.addTransformer(RaspClassFileTransformer.INSTANCE, true);
        inst.setNativeMethodPrefix(RaspClassFileTransformer.INSTANCE, EventWeaver.NATIVE_PREFIX);
    }

    /**
     * 获取模块管理器
     *
     * @return 模块管理器
     */
    public DefaultCoreModuleManager getCoreModuleManager() {
        return coreModuleManager;
    }

    // 创建运行时插件目录
    private void initPidRunDir() {
        mkdirs(this.cfg.getProcessPidPath());
        try {
            Runtime.getRuntime().addShutdownHook(clearThread);
        } catch (Exception e) {
            throw new RuntimeException("addShutdownHook error", e);
        }
    }

    private boolean mkdirs(String path) {
        // 预期在初始化时文件路径是不存在的
        File file = new File(path);
        if (!file.exists()) {
            return file.mkdirs();
        }
        throw new RuntimeException("mkdir file path : " + path + " exists.");
    }

    /**
     * 销毁沙箱
     */
    public void destroy() {

        // 卸载所有的模块
        coreModuleManager.unloadAll();

        // 清除 ClassFileTransformer
        inst.removeTransformer(RaspClassFileTransformer.INSTANCE);

        // 清理Spy
        SpyUtils.clean(cfg.getNamespace());

        cleanPidRunDir();
        // 清理线程
        if (clearThread != null) {
            // bugfix: 必须置为空，否则内存泄漏，嗯嗯
            Runtime.getRuntime().removeShutdownHook(clearThread);
            clearThread = null;
        }
    }

    // jrasp卸载时删除运行时插件pid目录
    private void cleanPidRunDir() {
        FileUtils.deleteQuietly(new File(this.cfg.getProcessPidPath()));
    }

}
