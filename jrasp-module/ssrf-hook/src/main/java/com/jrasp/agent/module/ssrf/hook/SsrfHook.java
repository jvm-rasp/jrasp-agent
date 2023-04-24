package com.jrasp.agent.module.ssrf.hook;

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
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author jrasp
 * 支持的中间件：okhttp2、okhttp3、httpclient、socket
 */
@MetaInfServices(Module.class)
@Information(id = "ssrf-hook", author = "jrasp")
public class SsrfHook extends ModuleLifecycleAdapter implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private String metaInfo;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String TYPE = "ssrf";

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        ssrfHook();
    }

    /**
     * @see java.net.Socket#connect
     */
    public void ssrfHook() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * socket
                 */
                .onClass(new ClassMatcher("java/net/Socket")
                        .onMethod(new MethodMatcher("(Ljava/net/SocketAddress;I)V",
                                        new AdviceListener() {
                                            @Override
                                            protected void before(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                SocketAddress socketAddress = (SocketAddress) advice.getParameterArray()[0];
                                                if (socketAddress instanceof InetSocketAddress) {
                                                    String hostName = ((InetSocketAddress) socketAddress).getHostName();
                                                    int port = ((InetSocketAddress) socketAddress).getPort();
                                                    algorithmManager.doCheck(TYPE, context.get(), hostName, port);
                                                }
                                            }

                                            @Override
                                            protected void afterThrowing(Advice advice) throws Throwable {
                                                // 方法调用完成，如果抛出异常（插桩的代码bug导致的异常或者主动阻断的异常）将清除上下文环境变量
                                                context.remove();
                                            }
                                        }
                                )
                        )
                )
                /**
                 * okhttp3的ssrf检测hook点
                 * @see okhttp3.internal.http.RealInterceptorChain#proceed
                 * 已经测试： 2022-12-06
                 */
                .onClass(new ClassMatcher("okhttp3/internal/http/RealInterceptorChain")
                        .onMethod(new MethodMatcher("proceed(Lokhttp3/Request;)Lokhttp3/Response;",
                                        new AdviceListener() {
                                            @Override
                                            protected void before(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                okhttp3.Request request = (okhttp3.Request) advice.getParameterArray()[0];
                                                if (request != null) {
                                                    okhttp3.HttpUrl url = request.url();
                                                    String host = url.host();
                                                    int port = url.port();
                                                    Map<String, Object> params = new HashMap<String, Object>();
                                                    params.put("url", url.toString());
                                                    params.put("host", host);
                                                    params.put("port", port);
                                                    params.put("funtion", "okhttp3");
                                                    algorithmManager.doCheck(TYPE, context.get(), params);
                                                }
                                            }

                                            @Override
                                            protected void afterThrowing(Advice advice) throws Throwable {
                                                // 方法调用完成，如果抛出异常（插桩的代码bug导致的异常或者主动阻断的异常）将清除上下文环境变量
                                                context.remove();
                                            }
                                        }
                                )
                        )
                )
                /**
                 * okhttp2的ssrf检测hook点
                 *  okhttp2.2 版本以上
                 * @see com.squareup.okhttp.Call.ApplicationInterceptorChain#proceed(com.squareup.okhttp.Request)
                 */
                .onClass(new ClassMatcher("com/squareup/okhttp/Call$ApplicationInterceptorChain")
                        .onMethod(new MethodMatcher("proceed(Lcom/squareup/okhttp/Request;)Lcom/squareup/okhttp/Response;",
                                        new AdviceListener() {
                                            @Override
                                            protected void before(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                com.squareup.okhttp.Request request = (com.squareup.okhttp.Request) advice.getParameterArray()[0];
                                                if (request != null) {
                                                    java.net.URL url = request.url();
                                                    String host = url.getHost();
                                                    int port = url.getPort();
                                                    Map<String, Object> params = new HashMap<String, Object>();
                                                    params.put("url", url.toString());
                                                    params.put("host", host);
                                                    params.put("port", port);
                                                    params.put("funtion", "okhttp2");
                                                    algorithmManager.doCheck(TYPE, context.get(), params);
                                                }
                                            }

                                            @Override
                                            protected void afterThrowing(Advice advice) throws Throwable {
                                                // 方法调用完成，如果抛出异常（插桩的代码bug导致的异常或者主动阻断的异常）将清除上下文环境变量
                                                context.remove();
                                            }
                                        }
                                )
                        )
                )
                /**
                 *  httpclient 的ssrf检测hook点
                 * @see org.apache.http.impl.client.CloseableHttpClient#execute
                 */
                .onClass(new ClassMatcher("org/apache/http/impl/client/CloseableHttpClient")
                        .onMethod(new MethodMatcher("execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                        new AdviceListener() {
                                            @Override
                                            protected void before(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                org.apache.http.HttpHost host = (org.apache.http.HttpHost) advice.getParameterArray()[0];
                                                if (host != null) {
                                                    HashMap<String, Object> params = getSsrfParam(host.toString(), host.getHostName(), host.getPort(), "httpclient");
                                                    algorithmManager.doCheck(TYPE, context.get(), params);
                                                }
                                            }

                                            @Override
                                            protected void afterThrowing(Advice advice) throws Throwable {
                                                context.remove();
                                            }
                                        }
                                )
                        )
                )
                /**
                 *  httpclient的redirect 检测hook点1
                 * @see org.apache.http.impl.client.DefaultRedirectStrategy#getLocationURI
                 */
                .onClass(new ClassMatcher("org/apache/http/impl/client/DefaultRedirectStrategy")
                        .onMethod(new MethodMatcher("getLocationURI(Lorg/apache/http/HttpRequest;Lorg/apache/http/HttpResponse;Lorg/apache/http/protocol/HttpContext;)Ljava/net/URI;",
                                        new AdviceListener() {
                                            @Override
                                            protected void afterReturning(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                java.net.URL url = (java.net.URL) advice.getReturnObj();
                                                if (url != null) {
                                                    context.get().addObject("redirectUrl",url);
                                                }
                                            }

                                            @Override
                                            protected void afterThrowing(Advice advice) throws Throwable {
                                                context.remove();
                                            }
                                        }
                                )
                        )
                )
                /**
                 * httpclient的redirect 检测hook点2
                 * @see org.apache.http.impl.client.DefaultRedirectHandler#getLocationURI
                 */
                .onClass(new ClassMatcher("org/apache/http/impl/client/DefaultRedirectHandler")
                        .onMethod(new MethodMatcher("getLocationURI(Lorg/apache/http/HttpResponse;Lorg/apache/http/protocol/HttpContext;)Ljava/net/URI;",
                                        new AdviceListener() {
                                            @Override
                                            protected void afterReturning(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                java.net.URL url = (java.net.URL) advice.getReturnObj();
                                                if (url != null) {
                                                    context.get().addObject("redirectUrl",url);
                                                }
                                            }

                                            @Override
                                            protected void afterThrowing(Advice advice) throws Throwable {
                                                // 方法调用完成，如果抛出异常（插桩的代码bug导致的异常或者主动阻断的异常）将清除上下文环境变量
                                                context.remove();
                                            }
                                        }
                                )
                        )
                )
                .build();
    }

    static HashMap<String, Object> getSsrfParam(String url, String hostname, int port, String function) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("url", url);
        params.put("hostname", hostname);
        params.put("function", function);
        params.put("port", port);
        LinkedList<String> ip = getIpList(hostname);
        Collections.sort(ip);
        params.put("ip", ip);
        return params;
    }

    public static LinkedList<String> getIpList(String hostname) {
        LinkedList<String> ip = new LinkedList<String>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            for (InetAddress address : addresses) {
                if (address != null && address instanceof Inet4Address) {
                    ip.add(address.getHostAddress());
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return ip;
    }

}
