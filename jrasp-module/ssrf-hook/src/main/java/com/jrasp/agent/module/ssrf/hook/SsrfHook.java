package com.jrasp.agent.module.ssrf.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.Reflection;
import com.squareup.okhttp.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.internal.http.RealInterceptorChain;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.kohsuke.MetaInfServices;

import java.net.*;
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
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String TYPE = "ssrf";

    @RaspResource
    private ThreadLocal<Context> requestContext;

    private static final ThreadLocal<URI> httpClientUriCache = new ThreadLocal<URI>() {
        @Override
        protected URI initialValue() {
            return null;
        }
    };

    private static final ThreadLocal<URL> URLConnectionURLCache = new ThreadLocal<URL>() {
        @Override
        protected URL initialValue() {
            return null;
        }
    };

    private static final ThreadLocal<Boolean> isChecking = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

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
        hookHttpClient();
        hookCommonHttpClient();
        hookURLConnectionRedirect();
//        hookURLOpenConnection();
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * socket
                 */
                .onClass(new ClassMatcher("java/net/Socket")
                        .onMethod("connect(Ljava/net/SocketAddress;I)V", new AdviceListener() {
                                    @Override
                                    protected void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        SocketAddress socketAddress = (SocketAddress) advice.getParameterArray()[0];
                                        if (socketAddress instanceof InetSocketAddress) {
                                            String hostName = ((InetSocketAddress) socketAddress).getHostName();
                                            int port = ((InetSocketAddress) socketAddress).getPort();
                                            Map<String, Object> params = new HashMap<String, Object>();
                                            params.put("host", hostName);
                                            params.put("port", port);
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
                /**
                 * okhttp3的ssrf检测hook点
                 * @see RealInterceptorChain#proceed
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
                                                Request request = (Request) advice.getParameterArray()[0];
                                                if (request != null) {
                                                    HttpUrl url = request.url();
                                                    String host = url.host();
                                                    int port = url.port();
                                                    Map<String, Object> params = new HashMap<String, Object>();
                                                    params.put("url", url.toString());
                                                    params.put("host", host);
                                                    params.put("port", port);
                                                    params.put("function", "okhttp3");
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
                 * @see Call.ApplicationInterceptorChain#proceed(com.squareup.okhttp.Request)
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
                                                    URL url = request.url();
                                                    String host = url.getHost();
                                                    int port = url.getPort();
                                                    Map<String, Object> params = new HashMap<String, Object>();
                                                    params.put("url", url.toString());
                                                    params.put("host", host);
                                                    params.put("port", port);
                                                    params.put("function", "okhttp2");
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
                .build();
    }

    private void hookHttpClient() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 *  httpclient 的ssrf检测hook点
                 * @see CloseableHttpClient#execute
                 */
                .onClass(new ClassMatcher("org/apache/http/impl/client/CloseableHttpClient")
                        .onMethod(new String[]{
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/client/methods/CloseableHttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/client/methods/CloseableHttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/client/methods/CloseableHttpResponse;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;)Lorg/apache/http/client/methods/CloseableHttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;",
                        }, new httpClientListener())
                )
                .onClass(new ClassMatcher("org/apache/http/impl/client/AutoRetryHttpClient")
                        .onMethod(new String[]{
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;"
                        }, new httpClientListener()))
                .onClass(new ClassMatcher("org/apache/http/impl/client/DecompressingHttpClient")
                        .onMethod(new String[]{
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/HttpResponse;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/client/methods/HttpUriRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;)Ljava/lang/Object;",
                                "execute(Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object;"
                        }, new httpClientListener()))
                .build();
    }

    public class httpClientListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            Object[] params = advice.getParameterArray();
            if (params.length == 0) {
                return;
            }
            if (params[0].getClass().getName().startsWith("org.apache.http.client.methods.HttpUriRequest")) {
                Object uriValue = params[0];
                if (!isChecking.get() && uriValue != null) {
                    isChecking.set(Boolean.TRUE);
                    URI uri = (URI) Reflection.invokeMethod(uriValue, "getURI", new Class[0], new Object[0]);
                    algorithmManager.doCheck(TYPE, context.get(), getSsrfParamFromURI(uri));
                }
            } else if (params[0].getClass().getName().startsWith("org.apache.http.HttpHost")) {
                Object host = params[0];
                if (!isChecking.get() && host != null) {
                    isChecking.set(Boolean.TRUE);
                    algorithmManager.doCheck(TYPE, context.get(), getSsrfParamFromHostValue(host));
                }
            } else if (params[0].getClass().getSuperclass().getName().startsWith("org.apache.http.client.methods.HttpRequestBase")) {
                //params[0] advice.getReturnObj() advice
                Object uriValue = params[0];
                Object response = advice.getReturnObj();
                try {
                    if (isChecking.get() && response != null) {
                        URI redirectUri = httpClientUriCache.get();
                        if (redirectUri != null) {
                            HashMap<String, Object> ssrfparams = getSsrfParamFromURIAndHost(uriValue);
                            if (ssrfparams != null) {
                                HashMap<String, Object> redirectParams = getSsrfParamFromURI(redirectUri);
                                if (redirectParams != null) {
                                    prepareRedirectParams(ssrfparams, redirectParams, response);
                                    Map<String, Object> paramsCheck = ParamUtils.getRedirectParams(advice, ssrfparams);
                                    // doCheck paramsCheck, true
                                    // redirectUrl paramsCheck.get("url2")
                                    // payload (String) paramsCheck.get("url")

                                }
                            }
                        }
                    }
                } finally {
                    isChecking.set(Boolean.FALSE);
                    httpClientUriCache.remove();
                }
            }
                                        /*org.apache.http.HttpHost host = (org.apache.http.HttpHost) advice.getParameterArray()[0];
                                        if (host != null) {
                                            HashMap<String, Object> params = getSsrfParam(host.toString(), host.getHostName(), host.getPort(), "httpclient");
                                            algorithmManager.doCheck(TYPE, context.get(), params);
                                        }*/
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            context.remove();
        }
    }

    private void hookHttpClientRedirect() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 *  httpclient的redirect 检测hook点1
                 * @see DefaultRedirectStrategy#getLocationURI
                 */
                .onClass(new ClassMatcher("org/apache/http/impl/client/DefaultRedirectStrategy")
                        .onMethod(new MethodMatcher("getLocationURI(Lorg/apache/http/HttpRequest;Lorg/apache/http/HttpResponse;Lorg/apache/http/protocol/HttpContext;)Ljava/net/URI;",
                                        new AdviceListener() {
                                            @Override
                                            protected void afterReturning(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                URL url = (URL) advice.getReturnObj();
                                                if (url != null) {
                                                    context.get().addObject("redirectUrl", url);
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
                 * @see DefaultRedirectHandler#getLocationURI
                 */
                .onClass(new ClassMatcher("org/apache/http/impl/client/DefaultRedirectHandler")
                        .onMethod(new MethodMatcher("getLocationURI(Lorg/apache/http/HttpResponse;Lorg/apache/http/protocol/HttpContext;)Ljava/net/URI;",
                                        new AdviceListener() {
                                            @Override
                                            protected void afterReturning(Advice advice) throws Throwable {
                                                if (disable) {
                                                    return;
                                                }
                                                URL url = (URL) advice.getReturnObj();
                                                if (url != null) {
                                                    context.get().addObject("redirectUrl", url);
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

    private void hookCommonHttpClient() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("org/apache/commons/httpclient/URI")
                        .onMethod("parseUriReference(Ljava/lang/String;Z)V", new AdviceListener() {
                            @Override
                            protected void after(Advice advice) throws Throwable {
                                Object obj = advice.getTarget();
                                String url = (String) advice.getParameterArray()[0];
                                if (obj == null) {
                                    return;
                                }
                                try {
                                    String host = Reflection.invokeStringMethod(obj, "getHost", new Class[0], new Object[0]);
                                    String port = null;
                                    Integer portInt = (Integer) Reflection.invokeMethod(obj, "getPort", new Class[0], new Object[0]);
                                    if (portInt != null && portInt > 0) {
                                        port = String.valueOf(portInt);
                                    }
                                    if (host != null) {
                                        Map<String, Object> params = ParamUtils.getSsrfParam(url, host, port, "commons_httpclient");
                                        algorithmManager.doCheck(TYPE, context.get(), params);
                                    }
                                } catch (ProcessControlException e) {
                                    throw e;
                                } catch (Throwable e) {
                                    logger.warning("parse url " + url + " failed: " + e.getMessage());
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        }))
                .build();
    }

    private void hookURLConnectionRedirect() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("sun/net/www/protocol/http/HttpURLConnection")
                        .onMethod("followRedirect()Z", new AdviceListener() {

                            @Override
                            protected void after(Advice advice) throws Throwable {
                                Boolean isRedirect = (Boolean) advice.getReturnObj();
                                Object obj = advice.getTarget();
                                if (isRedirect) {
                                    try {
                                        URLConnectionURLCache.set((URL) Reflection.invokeMethod(obj, "getURL", new Class[0], new Object[0]));
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        }))
                .build();
    }

    private void hookURLOpenConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("java/net/URL")
                        .onMethod(new String[]{
                                "openConnection()Ljava/net/URLConnection;",
                                "openConnection(Ljava/net/Proxy;)Ljava/net/URLConnection;"
                        }, new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                URL url = (URL) advice.getTarget();
                                if (url.getProtocol().equals("jar") || url.getProtocol().equals("file")) {
                                    return;
                                }
                                Map<String, Object> params = getSsrfParamFromURL(url);
                                System.out.println(params);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        }))
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

    private HashMap<String, Object> getSsrfParamFromURI(URI uri) {
        if (uri != null) {
            String url = null;
            String hostName = null;
            String port = "";
            try {
                url = uri.toString();
                hostName = uri.toURL().getHost();
                int temp = uri.toURL().getPort();
                if (temp > 0) {
                    port = temp + "";
                }
            } catch (Throwable t) {
                return null;
            }
            if (hostName != null) {
                return ParamUtils.getSsrfParam(url, hostName, port, "httpclient");
            }
        }
        return null;
    }

    private HashMap<String, Object> getSsrfParamFromURL(URL url) {
        try {
            String host = null;
            String port = "";
            if (url != null) {
                host = url.getHost();
                int temp = url.getPort();
                if (temp > 0) {
                    port = temp + "";
                }
            }
            if (url != null) {
                if (host != null) {
                    return ParamUtils.getSsrfParam(url.toString(), host, port, "url_open_connection");
                }
                return ParamUtils.getSsrfParam(url.toString(), "", "-1", "url_open_connection");
            }
        } catch (Exception exception) {
        }
        return null;
    }

    private HashMap<String, Object> getSsrfParamFromHostValue(Object host) {
        try {
            String hostname = Reflection.invokeStringMethod(host, "getHostName", new Class[0], new Object[0]);
            String port = "";
            Integer portValue = (Integer) Reflection.invokeMethod(host, "getPort", new Class[0], new Object[0]);
            if (portValue != null && portValue.intValue() > 0) {
                port = portValue.toString();
            }
            if (hostname != null) {
                return ParamUtils.getSsrfParam(host.toString(), hostname, port, "httpclient");
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private HashMap<String, Object> getSsrfParamFromURIAndHost(Object value) throws Exception {
        if (value.getClass().getName().contains("HttpHost")) {
            return getSsrfParamFromHostValue(value);
        }
        URI uri = (URI) Reflection.invokeMethod(value, "getURI", new Class[0], new Object[0]);
        return getSsrfParamFromURI(uri);
    }

    private void prepareRedirectParams(HashMap<String, Object> params, HashMap<String, Object> redirectParams, Object response) throws Exception {
        params.put("url2", redirectParams.get("url"));
        params.put("hostname2", redirectParams.get("hostname"));
        params.put("port2", redirectParams.get("port"));
        params.put("ip2", redirectParams.get("ip"));
        Object statusLine = Reflection.invokeMethod(response, "getStatusLine", new Class[0], new Object[0]);
        if (statusLine != null) {
            String statusMsg = Reflection.invokeStringMethod(statusLine, "getReasonPhrase", new Class[0], new Object[0]);
            statusMsg = (statusMsg == null) ? "" : statusMsg;
            params.put("http_message", statusMsg);
            int statusCode = ((Integer) Reflection.invokeMethod(statusLine, "getStatusCode", new Class[0], new Object[0])).intValue();
            statusCode = Math.max(statusCode, 0);
            params.put("http_status", Integer.valueOf(statusCode));
        }
    }
}
