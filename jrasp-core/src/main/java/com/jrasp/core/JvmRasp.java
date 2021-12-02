package com.jrasp.core;

import com.jrasp.core.enhance.weaver.EventListenerHandler;
import com.jrasp.core.manager.CoreModuleManager;
import com.jrasp.core.manager.impl.DefaultCoreLoadedClassDataSource;
import com.jrasp.core.manager.impl.DefaultCoreModuleManager;
import com.jrasp.core.manager.impl.DefaultProviderManager;
import com.jrasp.core.util.RaspProtector;
import com.jrasp.core.util.SpyUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JvmRasp {

    private final static List<String> earlyLoadRaspClassNameList = new ArrayList<String>();

    static {
        earlyLoadRaspClassNameList.add("com.jrasp.core.util.RaspClassUtils");
        earlyLoadRaspClassNameList.add("com.jrasp.core.util.matcher.structure.ClassStructureImplByAsm");
    }

    private final CoreConfigure cfg;
    private final CoreModuleManager coreModuleManager;

    private Thread clearThread = new Thread(new Runnable() {
        @Override
        public void run() {
            FileUtils.deleteQuietly(new File(cfg.getProcessPidPath()));
        }
    }, "jvm-shutdown-clear-thread");

    public JvmRasp(final CoreConfigure cfg,
                   final Instrumentation inst) {
        EventListenerHandler.getSingleton();
        this.cfg = cfg;
        this.coreModuleManager = RaspProtector.instance.protectProxy(CoreModuleManager.class, new DefaultCoreModuleManager(
                cfg,
                inst,
                new DefaultCoreLoadedClassDataSource(inst, cfg.isEnableUnsafe()),
                new DefaultProviderManager(cfg)
        ));

        init();
    }

    private void init() {
        // 输出技术支持链接，方便业务排查问题
        System.out.println(String.format("%s [jrasp] %s %s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss").format(new Date()),
                "开启RASP安全防护，技术支持:", cfg.getSupportURL()));

        initPidRunDir();

        doEarlyLoadRaspClass();

        SpyUtils.init(cfg.getNamespace());
    }

    // 创建运行时插件目录
    private void initPidRunDir() {
        mkdirs(cfg.getProcessPidPath());
        mkdirs(cfg.getRuntimeSystemModulePath());
        mkdirs(cfg.getRuntimeRequiredModulePath());
        mkdirs(cfg.getRuntimeOptionalModulePath());
        // JVM 退出时删除目录
        try {
            Runtime.getRuntime().addShutdownHook(clearThread);
        } catch (Exception e) {
            throw new RuntimeException("addShutdownHook error", e);
        }
    }

    // jrasp卸载时删除运行时插件pid目录
    private void cleanPidRunDir() {
        FileUtils.deleteQuietly(new File(cfg.getProcessPidPath()));
    }

    private boolean mkdirs(String path) {
        // 预期在初始化时文件路径是不存在的
        File file = new File(path);
        if (!file.exists()) {
            return file.mkdirs();
        }
        throw new RuntimeException("mkdir file path : " + path + " exists.");
    }

    // 提前加载某些必要的类
    private void doEarlyLoadRaspClass() {
        for (String className : earlyLoadRaspClassNameList) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                //加载rasp内部的类，不可能加载不到
            }
        }
    }

    /**
     * 获取模块管理器
     */
    public CoreModuleManager getCoreModuleManager() {
        return coreModuleManager;
    }

    /**
     * 销毁沙箱
     */
    public void destroy() {
        // 输出技术支持链接，方便业务排查问题
        System.out.println("关闭RASP安全防护,技术支持:" + cfg.getSupportURL());

        if (clearThread != null) {
            Runtime.getRuntime().removeShutdownHook(clearThread);
        }

        // 卸载所有的模块
        coreModuleManager.unloadAll();

        // 清理Spy
        SpyUtils.clean(cfg.getNamespace());

        // 清除目录
        cleanPidRunDir();

    }

}
