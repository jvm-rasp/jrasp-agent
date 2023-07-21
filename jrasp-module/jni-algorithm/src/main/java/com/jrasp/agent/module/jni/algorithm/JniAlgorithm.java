package com.jrasp.agent.module.jni.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.ProcessController;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StackTrace;
import com.jrasp.agent.api.util.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@MetaInfServices(Module.class)
@Information(id = "jni-algorithm", author = "yhlong")
public class JniAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private String metaInfo;

    private volatile Integer jniAction = 0;

    private Set<String> libraryWhiteStackList = new HashSet<String>(Arrays.asList(
            "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
            "sun.awt.image.NativeLibLoader.loadLibraries",
            "java.awt.Toolkit",
            "java.awt.image.ColorModel",
            "com.sun.imageio.plugins.jpeg.JPEGImageReader",
            "sun.java2d.cmm.lcms.LCMS",
            "sun.font.FontManagerNativeLibrary",
            "sun.font.T2KFontScaler"
    ));

    @Override
    public boolean update(Map<String, String> configMaps) {
        algorithmManager.register(this);
        this.jniAction = ParamSupported.getParameter(configMaps, "jni_action", Integer.class, jniAction);
        this.libraryWhiteStackList = ParamSupported.getParameter(configMaps, "library_white_stack_list", Set.class, libraryWhiteStackList);
        return true;
    }

    @Override
    public String getType() {
        return "jni";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (isWhiteList(context)) {
            return;
        }
        boolean enableBlock = jniAction == 1;
        String libPath = (String) parameters[0];
        String message = "detect jni loadLibrary, libPath: " + libPath;
        AttackInfo attackInfo = new AttackInfo(
                context,
                metaInfo,
                libPath,
                enableBlock,
                "JNI类库加载",
                getType(),
                message,
                60);
        logger.attack(attackInfo);
        if (enableBlock) {
            ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("jni loadLibrary block by JRASP."));
        }
    }

    private boolean isWhiteList(Context context) {
        if (context != null
                && StringUtils.isBlank(context.getMethod())
                && StringUtils.isBlank(context.getRequestURI())
                && StringUtils.isBlank(context.getRequestURL())) {
            return true;
        }
        for (String stack : StackTrace.getStackTraceString()) {
            for (String keyword : libraryWhiteStackList) {
                if (stack.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getDescribe() {
        return "detect jni loadLibrary";
    }
}
