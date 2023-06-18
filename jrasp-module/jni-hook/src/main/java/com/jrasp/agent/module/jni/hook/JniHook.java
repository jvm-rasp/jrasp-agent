package com.jrasp.agent.module.jni.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.util.Map;

@MetaInfServices(Module.class)
@Information(id = "jni-hook", author = "yhlong")
public class JniHook implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String TYPE = "jni";

    @Override
    public void loadCompleted() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("java/lang/System")
                        .onMethod("load(Ljava/lang/String;)V", new loadLibraryListener())
                        .onMethod("loadLibrary(Ljava/lang/String;)V", new loadLibraryListener()))
                .onClass(new ClassMatcher("java/lang/ClassLoader")
                        .onMethod("loadLibrary0(Ljava/lang/Class;Ljava/io/File;)Z", new loadLibrary0Listener()))
                .onClass(new ClassMatcher("java/lang/ClassLoader$NativeLibrary")
                        .onMethod("<init>", new nativeLibraryListener()))
                .build();
    }

    public class loadLibraryListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }

            String libPath = (String) advice.getParameterArray()[0];
            algorithmManager.doCheck(TYPE, context.get(), libPath);
        }
    }

    public class loadLibrary0Listener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }

            File file;
            if (advice.getParameterArray().length == 2) {
                file = (File) advice.getParameterArray()[1];
            } else {
                file = (File) advice.getParameterArray()[0];
            }
            algorithmManager.doCheck(TYPE, context.get(), file.getAbsolutePath());
        }
    }

    public class nativeLibraryListener extends AdviceListener {

        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }

            String libPath = (String) advice.getParameterArray()[0];
            algorithmManager.doCheck(TYPE, context.get(), libPath);
        }
    }

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }
}
