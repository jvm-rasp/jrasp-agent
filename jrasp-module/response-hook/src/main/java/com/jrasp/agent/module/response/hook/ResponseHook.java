package com.jrasp.agent.module.response.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.request.HttpServletResponse;
import com.jrasp.agent.api.util.Reflection;
import org.kohsuke.MetaInfServices;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.jrasp.agent.api.util.ParamSupported.getParameter;

/**
 * @author jrasp
 */
@MetaInfServices(Module.class)
@Information(id = "response-hook", author = "jrasp")
public class ResponseHook extends ModuleLifecycleAdapter implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile boolean disable = false;

    private final static String TYPE = "response";

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        responseBodyHook();
    }

    public void responseBodyHook() {
        new EventWatchBuilder(moduleEventWatcher)
                // tomcat
                .onClass(new ClassMatcher("org/apache/coyote/Response").
                        onMethod(
                                new MethodMatcher("doWrite(Ljava/nio/ByteBuffer;)V", new TomcatAdviceListener())
                        ).
                        onMethod(
                                new MethodMatcher("doWrite(Lorg/apache/tomcat/util/buf/ByteChunk;)V", new TomcatAdviceListener())
                        )
                )
                // jetty

                // spark


                // undertown
                .build();
    }

    public class TomcatAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            Object response = advice.getTarget();
            Object trunk = advice.getParameterArray()[0];
            if (response != null && trunk != null) {
                HashMap<String, Object> params = new HashMap<String, Object>();
                Object originResponse = context.get().getResponse();
                HttpServletResponse wrapperResponse = new HttpServletResponse(originResponse);

                String enc = wrapperResponse.getCharacterEncoding();
                String contentType = wrapperResponse.getContentType();

                if (enc != null) {
                    params.put("buffer", trunk);
                    params.put("content_length", Reflection.invokeMethod(response, "getContentLength", new Class[]{}));
                    params.put("encoding", enc);
                    params.put("content_type", contentType);
                    if (trunk instanceof java.nio.ByteBuffer) {
                        params.put("content", getContentFromByteBuffer(trunk, enc));
                    } else {
                        params.put("content", getContentFromByteTrunk(trunk, enc));
                    }
                    algorithmManager.doCheck(TYPE, context.get(), params);
                }
            }
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            context.remove();
        }

        private String getContentFromByteBuffer(Object trunkObject, String enc) throws UnsupportedEncodingException {
            java.nio.ByteBuffer trunk = (java.nio.ByteBuffer) trunkObject;
            byte[] bytes = trunk.array();
            int end = trunk.limit();
            int start = trunk.position();
            byte[] tmp = new byte[end - start];
            System.arraycopy(bytes, start, tmp, 0, end - start);
            return new String(tmp, enc);
        }

        private String getContentFromByteTrunk(Object trunk, String enc) throws Exception {
            byte[] bytes = (byte[]) Reflection.invokeMethod(trunk, "getBuffer", new Class[]{});
            int start = (Integer) Reflection.invokeMethod(trunk, "getStart", new Class[]{});
            int end = (Integer) Reflection.invokeMethod(trunk, "getEnd", new Class[]{});
            byte[] tmp = new byte[end - start];
            System.arraycopy(bytes, start, tmp, 0, end - start);
            return new String(tmp, enc);
        }
    }

}