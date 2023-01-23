package com.jrasp.agent.module.http.hook;

import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StringUtils;
import io.undertow.util.HeaderValues;
import org.kohsuke.MetaInfServices;

import java.io.InputStream;
import java.util.*;

/**
 * http 的hook点
 * 支持的web容器：jetty、tomcat、undertown、spark
 */
@MetaInfServices(Module.class)
@Information(id = "http-hook", author = "jrasp")
public class HttpHook extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> requestContext;

    private final static String TYPE = "http";

    /**
     * hook开关，默认开启，可以在管理端统一配置
     */
    private volatile Boolean disable = false;

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    @Override
    public void loadCompleted() {
        requestHook();
    }

    public void requestHook() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * dispatchRequest 请求预处理hook点；
                 * 用来清除 requestInfo 信息
                 * @see ServletInitialHandler#dispatchRequest(HttpServerExchange, ServletRequestContext, ServletChain, javax.servlet.DispatcherType)
                 */
                .onClass(new ClassMatcher("io/undertow/servlet/handlers/ServletInitialHandler")
                        .onMethod("dispatchRequest(Lio/undertow/server/HttpServerExchange;Lio/undertow/servlet/handlers/ServletRequestContext;Lio/undertow/servlet/handlers/ServletChain;Ljavax/servlet/DispatcherType;)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                requestContext.remove(); // 清除 requestInfo 信息
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                        /**
                         * 绑定 request http 参数
                         * @see ServletInitialHandler#handleFirstRequest(HttpServerExchange, ServletRequestContext)
                         */
                        .onMethod("handleFirstRequest(Lio/undertow/server/HttpServerExchange;Lio/undertow/servlet/handlers/ServletRequestContext;)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                Context context = requestContext.get();
                                io.undertow.server.HttpServerExchange exchange = (io.undertow.server.HttpServerExchange) advice.getParameterArray()[0];
                                storeRequestInfo(context, exchange);
                                // 参数检查
                                algorithmManager.doCheck(TYPE, context, null);
                            }

                            @Override
                            public void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                )
                /**
                 * 绑定 request http body参数
                 * ServletInputStreamImpl.read():从包含的输入流中读取len个字节并将它们分配在缓冲b起始于b[off]
                 * @see ServletInputStreamImpl#read(byte[], int, int)
                 */
                .onClass(new ClassMatcher("io/undertow/servlet/spec/ServletInputStreamImpl")
                        .onMethod("read([BII)I", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                int ret = (Integer) advice.getReturnObj();                // 流读取的结果，-1 表示失败；
                                if (ret == -1) {
                                    return;
                                }
                                Object inputStream = advice.getTarget(); // 数据源
                                byte[] bytes = (byte[]) advice.getParameterArray()[0];  //　缓冲器
                                int offset = (Integer) advice.getParameterArray()[1];   //　写入的偏移地址
                                onInputStreamRead(ret, inputStream, bytes, offset);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                )
                /**
                 * 绑定 request url请求参数
                 * @see io.undertow.server.HttpServerExchange#getQueryParameters
                 */
                .onClass(new ClassMatcher("io/undertow/server/HttpServerExchange")
                        .onMethod("getQueryParameters()Ljava/util/Map;", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                Map<String, String[]> storeParameters = new HashMap<String, String[]>();
                                Map<String, Deque<String>> queryParameters = (Map<String, Deque<String>>) advice.getReturnObj();
                                // 类型转换  Deque<String> =====> String[]
                                for (Map.Entry<String, Deque<String>> entry : queryParameters.entrySet()) {
                                    storeParameters.put(entry.getKey(), entry.getValue().toArray(new String[0]));
                                }
                                Context context = requestContext.get();
                                context.setParameterMap(storeParameters);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                )
                /** 这个是请求的起点，用来清除 context 信息
                 *
                 * @see org.apache.catalina.connector.CoyoteAdapter#service(org.apache.coyote.Request, org.apache.coyote.Response)
                 * 需要注意参数 Request、Response的包名称
                 */
                .onClass(new ClassMatcher("org/apache/catalina/connector/CoyoteAdapter")
                        .onMethod("service(Lorg/apache/coyote/Request;Lorg/apache/coyote/Response;)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                requestContext.remove(); // 清除 requestInfo 信息
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                )
                /**
                 * 绑定 request 参数
                 *
                 * @see org.apache.catalina.core.StandardWrapperValve#invoke(org.apache.catalina.connector.Request, org.apache.catalina.connector.Response)
                 * 需要注意参数 Request、Response的包名称
                 */
                .onClass(new ClassMatcher("org/apache/catalina/core/StandardWrapperValve")
                        .onMethod("invoke(Lorg/apache/catalina/connector/Request;Lorg/apache/catalina/connector/Response;)V", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                Context context = requestContext.get();
                                org.apache.catalina.connector.Request request = (org.apache.catalina.connector.Request) advice.getParameterArray()[0];
                                // 存储 response
                                org.apache.catalina.connector.Response response = (org.apache.catalina.connector.Response) advice.getParameterArray()[1];
                                requestContext.get().setResponse(response);
                                storeTomcatRequestInfo(context, request);
                                algorithmManager.doCheck(TYPE, requestContext.get(), null);
                            }

                            @Override
                            public void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                )
                .onClass(new ClassMatcher("org/apache/catalina/connector/InputBuffer")
                        /**
                         * 绑定 RequestBody  byte[] 类型
                         *
                         * @see org.apache.catalina.connector.InputBuffer#read(byte[], int, int)
                         */
                        .onMethod("read([BII)I", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) {
                                if (disable) {
                                    return;
                                }
                                int ret = (Integer) advice.getReturnObj();
                                // 流读取的结果，-1 表示失败；
                                if (ret == -1) {
                                    return;
                                }
                                byte[] bytes = (byte[]) advice.getParameterArray()[0];  //　缓冲器
                                int offset = (Integer) advice.getParameterArray()[1];   //　写入的偏移地址
                                // int length = (Integer) advice.getParameterArray()[2]; // 写入长度不需要
                                onInputStreamRead(ret, advice.getTarget(), bytes, offset);
                            }

                            @Override
                            public void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                        /**
                         * @see org.apache.catalina.connector.InputBuffer#readByte()
                         * 这里要加上readByte,否则丢失一个字节数据 如果输入是字符串 ABC 会变成 BC
                         */
                        .onMethod("readByte()I", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) {
                                if (disable) {
                                    return;
                                }
                                // 流读取的结果，-1 表示失败
                                int ret = (Integer) advice.getReturnObj();
                                if (ret == -1) {
                                    return;
                                }
                                //  写入一个字节
                                onInputStreamRead(ret, advice.getTarget());
                            }

                            @Override
                            public void afterThrowing(Advice advice) throws Throwable {
                                requestContext.remove();
                            }
                        })
                )
                /**
                 * @see org.eclipse.jetty.server.Server#handle(org.eclipse.jetty.server.HttpChannel) jetty9+
                 * @see org.eclipse.jetty.server.Server#handle(org.eclipse.jetty.server.AbstractHttpConnection) jetty8以下
                 */
                .onClass(new ClassMatcher("org/eclipse/jetty/server/Server")
                        .onMethod("handle(Lorg/eclipse/jetty/server/HttpChannel;)V", new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                // 清除上次请求的 requestInfo 信息,防止脏数据
                                requestContext.remove();
                                org.eclipse.jetty.server.HttpChannel httpChannel = (org.eclipse.jetty.server.HttpChannel) advice.getParameterArray()[0];
                                if (httpChannel != null) {
                                    org.eclipse.jetty.server.Request request = httpChannel.getRequest();
                                    Context context = requestContext.get();
                                    storeJettyRequestInfo(context, request);
                                    // 参数检查
                                    algorithmManager.doCheck(TYPE, requestContext.get(), null);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 context 信息，防止内存泄漏
                                requestContext.remove();
                            }
                        })
                )

                /**
                 * @see org.eclipse.jetty.server.HttpInput#read(byte[], int, int)
                 */
                .onClass(new ClassMatcher("org/eclipse/jetty/server/HttpInput")
                        .onMethod("read([BII)I", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                int ret = (Integer) advice.getReturnObj();                // 流读取的结果，-1 表示失败；
                                if (ret == -1) {
                                    return;
                                }
                                // 获取参数
                                InputStream inputStream = (InputStream) advice.getTarget();
                                byte[] bytes = (byte[]) advice.getParameterArray()[0];    // 方法的第1个参数
                                Integer offset = (Integer) advice.getParameterArray()[1]; // 方法的第2个参数，第三个参数没有用到
                                onInputStreamRead(ret, inputStream, bytes, offset);
                            }

                            @Override
                            public void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 requestInfo 信息，防止内存泄漏
                                requestContext.remove();
                            }
                        })
                )
                /**
                 * @see org.eclipse.jetty.server.Server#handle
                 */
                .onClass(new ClassMatcher("org/eclipse/jetty/server/Server")
                        .onMethod("handle(Lorg/eclipse/jetty/server/AbstractHttpConnection;)V", new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                // 清除上次请求的 requestInfo 信息,防止脏数据
                                requestContext.remove();
                                // 绑定本次请求的请求头
                                org.eclipse.jetty.server.AbstractHttpConnection connection = (org.eclipse.jetty.server.AbstractHttpConnection) advice.getParameterArray()[0];
                                if (connection != null) {
                                    org.eclipse.jetty.server.Request request = connection.getRequest();
                                    storeJettyRequestInfo(requestContext.get(), request);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 context 信息，防止内存泄漏
                                requestContext.remove();
                            }
                        })
                )

                /**
                 * spark 内置jetty
                 * @see org.sparkproject.jetty.server.Server#handle
                 */
                .onClass(new ClassMatcher("org/sparkproject/jetty/server/Server")
                        .onMethod("handle(Lorg/sparkproject/jetty/server/HttpChannel;)V", new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                // 清除上次请求的 requestInfo 信息,防止脏数据
                                requestContext.remove();
                                // 绑定本次请求的请求头
                                org.sparkproject.jetty.server.HttpChannel connection = (org.sparkproject.jetty.server.HttpChannel) advice.getParameterArray()[0];
                                if (connection != null) {
                                    org.sparkproject.jetty.server.Request request = connection.getRequest();
                                    storeSparkJettyRequestInfo(requestContext.get(), request);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 context 信息，防止内存泄漏
                                requestContext.remove();
                            }
                        })
                )
                /**
                 * spark 内置 jetty
                 * @see org.sparkproject.jetty.server.HttpInput#read(byte[], int, int)
                 */
                .onClass(new ClassMatcher("org/sparkproject/jetty/server/HttpInput")
                        .onMethod("read([BII)I", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                int ret = (Integer) advice.getReturnObj();                // 流读取的结果，-1 表示失败；
                                if (ret == -1) {
                                    return;
                                }
                                // 获取参数
                                InputStream inputStream = (InputStream) advice.getTarget();
                                byte[] bytes = (byte[]) advice.getParameterArray()[0];    // 方法的第1个参数
                                Integer offset = (Integer) advice.getParameterArray()[1]; // 方法的第2个参数，第三个参数没有用到
                                onInputStreamRead(ret, inputStream, bytes, offset);
                            }

                            @Override
                            public void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 requestInfo 信息，防止内存泄漏
                                requestContext.remove();
                            }
                        })
                )
                .build();
    }

    // bugfix: 方法参数不得涉及第三方类
    public static void storeJettyRequestInfo(Context context, Object object) {
        org.eclipse.jetty.server.Request request = (org.eclipse.jetty.server.Request) object;

        // 本机地址
        String localAddr = request.getLocalAddr();
        context.setLocalAddr(localAddr);

        // http请求类型：get、post
        String method = request.getMethod();
        context.setMethod(method);

        // body 长度
        int contentLength = request.getContentLength();
        context.setContentLength(contentLength);

        // content-type
        String contentType = request.getContentType();
        context.setContentType(contentType);

        // 获取responseContentType
        String responseContentType = request.getHeader("Accept");
        if (StringUtils.isBlank(responseContentType)) {
            responseContentType = request.getHeader("accept");
        }
        context.setResponseContentType(responseContentType);

        // query
        context.setQueryString(request.getQueryString());

        // http请求协议: HTTP/1.1
        String protocol = request.getProtocol();
        context.setProtocol(protocol);

        // 调用主机地址
        String remoteHost = request.getRemoteHost();
        context.setRemoteHost(remoteHost);

        // URL
        String requestURL = request.getRequestURL().toString();
        context.setRequestURL(requestURL);

        // URI
        String requestURI = request.getRequestURI();
        context.setRequestURI(requestURI);

        // parameters
        if ("get".equalsIgnoreCase(method) ||
                (StringUtils.isNotBlank(contentType) && contentType.contains("application/x-www-form-urlencoded"))) {
            // 可以调用 request.getParameterMap()
            // 如果不区分content-type 直接调用会导致严重bug
            Map<String, String[]> parameterMap = request.getParameterMap();
            context.setParameterMap(parameterMap);
        }

        // 请求header
        Map<String, String> header = new HashMap<String, String>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                header.put(key.toLowerCase(), value);
            }
        }
        context.setHeader(header);
    }

    public static void storeTomcatRequestInfo(Context context, Object object) {
        org.apache.catalina.connector.Request request = (org.apache.catalina.connector.Request) object;
        // 本机地址
        String localAddr = request.getLocalAddr();
        context.setLocalAddr(localAddr);

        // http请求类型：get、post
        String method = request.getMethod();
        context.setMethod(method);

        // body 长度
        int contentLength = request.getContentLength();
        context.setContentLength(contentLength);

        // content-type
        String contentType = request.getContentType();
        context.setContentType(contentType);

        // 获取responseContentType
        String responseContentType = request.getHeader("Accept");
        if (StringUtils.isBlank(responseContentType)) {
            responseContentType = request.getHeader("accept");
        }
        context.setResponseContentType(responseContentType);

        // query
        context.setQueryString(request.getQueryString());

        // http请求协议: HTTP/1.1
        String protocol = request.getProtocol();
        context.setProtocol(protocol);

        // 调用主机地址
        String remoteHost = request.getRemoteHost();
        context.setRemoteHost(remoteHost);

        // URL
        String requestURL = request.getRequestURL().toString();
        context.setRequestURL(requestURL);

        // URI
        String requestURI = request.getRequestURI();
        context.setRequestURI(requestURI);

        // parameters
        if ("get".equalsIgnoreCase(method) ||
                (StringUtils.isNotBlank(contentType) && contentType.contains("application/x-www-form-urlencoded"))) {
            // 可以调用 request.getParameterMap()
            // 如果不区分content-type 直接调用会导致严重bug
            Map<String, String[]> parameterMap = request.getParameterMap();
            context.setParameterMap(parameterMap);
        }

        // 请求header
        Map<String, String> header = new HashMap<String, String>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                header.put(key.toLowerCase(), value);
            }
        }
        context.setHeader(header);
    }

    public void storeRequestInfo(Context context, Object request) {
        io.undertow.server.HttpServerExchange exchange = (io.undertow.server.HttpServerExchange) request;
        // 本机地址
        String localAddr = exchange.getDestinationAddress().getAddress().getHostAddress();
        context.setLocalAddr(localAddr);

        // http请求类型：get、post
        String method = exchange.getRequestMethod().toString();
        context.setMethod(method);

        // URL
        String requestURL = exchange.getRequestURL();
        context.setRequestURL(requestURL);

        // URI
        String requestURI = exchange.getRequestURI();
        context.setRequestURI(requestURI);

        // http请求协议: HTTP/1.1
        String protocol = exchange.getProtocol().toString();
        context.setProtocol(protocol);

        // 调用主机地址
        String remoteHost = exchange.getSourceAddress().getAddress().getHostAddress();
        context.setRemoteHost(remoteHost);

        String queryString = exchange.getQueryString();
        context.setQueryString(queryString);

        // 请求header
        Map<String, String> header = new HashMap<String, String>(32);
        io.undertow.util.HeaderMap requestHeaders = exchange.getRequestHeaders();
        if (requestHeaders != null) {
            // 获取responseContentType
            HeaderValues accept = requestHeaders.get("Accept");
            if (accept != null) {
                context.setResponseContentType(accept.toString());
            }
            Iterator<io.undertow.util.HeaderValues> iterator = requestHeaders.iterator();
            while (iterator.hasNext()) {
                io.undertow.util.HeaderValues next = iterator.next();
                header.put(next.getHeaderName().toString(), toString(next.toArray()));
            }
        }
        context.setHeader(header);
    }

    public static void storeSparkJettyRequestInfo(Context context, Object object) {
        org.sparkproject.jetty.server.Request request = (org.sparkproject.jetty.server.Request) object;

        // 本机地址
        String localAddr = request.getLocalAddr();
        context.setLocalAddr(localAddr);

        // http请求类型：get、post
        String method = request.getMethod();
        context.setMethod(method);

        // body 长度
        int contentLength = request.getContentLength();
        context.setContentLength(contentLength);

        // content-type
        String contentType = request.getContentType();
        context.setContentType(contentType);

        // 获取responseContentType
        String responseContentType = request.getHeader("Accept");
        if (StringUtils.isBlank(responseContentType)) {
            responseContentType = request.getHeader("accept");
        }
        context.setResponseContentType(responseContentType);

        // query
        context.setQueryString(request.getQueryString());

        // http请求协议: HTTP/1.1
        String protocol = request.getProtocol();
        context.setProtocol(protocol);

        // 调用主机地址
        String remoteHost = request.getRemoteHost();
        context.setRemoteHost(remoteHost);

        // URL
        String requestURL = request.getRequestURL().toString();
        context.setRequestURL(requestURL);

        // URI
        String requestURI = request.getRequestURI();
        context.setRequestURI(requestURI);

        // parameters
        if ("get".equalsIgnoreCase(method) ||
                (StringUtils.isNotBlank(contentType) && contentType.contains("application/x-www-form-urlencoded"))) {
            // 可以调用 request.getParameterMap()
            // 如果不区分content-type 直接调用会导致严重bug
            Map<String, String[]> parameterMap = request.getParameterMap();
            context.setParameterMap(parameterMap);
        }

        // 请求header
        Map<String, String> header = new HashMap<String, String>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                header.put(key.toLowerCase(), value);
            }
        }
        context.setHeader(header);
    }

    // 写入一个byte
    public void onInputStreamRead(int ret, Object inputStream) {
        if (ret != -1 && requestContext.get() != null) {
            Context context = requestContext.get();
            if (context.getInputStream() == null) {
                context.setInputStream(inputStream);
            }
            if (context.getInputStream() == inputStream) {
                context.appendByteBody(ret);
            }
        }
    }

    public void onInputStreamRead(int ret, Object inputStream, byte[] bytes, int offset) {
        if (ret != -1 && requestContext.get() != null) {
            Context context = requestContext.get();
            if (context.getInputStream() == null) {
                context.setInputStream(inputStream);
            }
            if (context.getInputStream() == inputStream) {
                context.appendBody(bytes, offset, ret);
            }
        }
    }

    private String toString(String[] arrays) {
        final StringBuilder buf = new StringBuilder(64);
        for (int i = 0; i < arrays.length; i++) {
            if (i > 0) {
                buf.append(",");
            }
            if (arrays[i] != null) {
                buf.append(arrays[i]);
            }
        }
        return buf.toString();
    }
}
