package com.jrasp.core.server.jetty.servlet;

import com.alibaba.fastjson.JSONObject;
import com.jrasp.api.annotation.Command;
import com.jrasp.api.model.RestResultUtils;
import com.jrasp.core.CoreConfigure;
import com.jrasp.core.CoreModule;
import com.jrasp.core.CoreModule.ReleaseResource;
import com.jrasp.core.manager.CoreModuleManager;
import com.jrasp.core.manager.impl.JwtTokenServiceImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.jrasp.api.model.ResultCodeEnum.*;
import static com.jrasp.api.util.GaStringUtils.matching;

public class ModuleHttpServlet extends HttpServlet {

    // 无需token的接口
    private static final List<String> NO_LOGIN_PATHS = Arrays.asList("/user/login", "/user/update", "/info/version", "/module/list");

    private static final String SLASH = "/";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CoreConfigure cfg;
    private final CoreModuleManager coreModuleManager;

    public ModuleHttpServlet(final CoreConfigure cfg,
                             final CoreModuleManager coreModuleManager) {
        this.cfg = cfg;
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding(cfg.getServerCharset().name());
        doMethod(req, resp, Command.Method.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding(cfg.getServerCharset().name());
        doMethod(req, resp, Command.Method.POST);
    }

    private void doMethod(final HttpServletRequest req,
                          final HttpServletResponse resp,
                          final Command.Method expectHttpMethod) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        // 获取请求路径
        final String path = req.getPathInfo(); //例如： /module/list

        // 开启鉴权并且路径不在白名单之中
        if (cfg.getEnableAuth() && !NO_LOGIN_PATHS.contains(path)) {
            boolean authentication = JwtTokenServiceImpl.instance.verifyToken(req.getHeader("Authentication"));
            if (!authentication) {
                writer.println(JSONObject.toJSONString(RestResultUtils.failed(AUTH_ERROR)));
                return;
            }
        }

        // 获取模块ID
        final String uniqueId = parseUniqueId(path);
        if (StringUtils.isBlank(uniqueId)) {
            logger.warn("path={} is not matched any module.", path);
            writer.println(JSONObject.toJSONString(RestResultUtils.failed(NOT_FOUND,"path=%s is not matched any module",path)));
            return;
        }

        // 获取模块
        final CoreModule coreModule = coreModuleManager.get(uniqueId);
        if (null == coreModule) {
            logger.warn("path={} is matched module {}, but not existed.", path, uniqueId);
            writer.println(JSONObject.toJSONString(RestResultUtils.failed(NOT_FOUND,"path=%s is matched module %s, but not existed.",path,uniqueId)));
            return;
        }

        // 匹配对应的方法
        final Method method = matchingModuleMethod(
                path,
                expectHttpMethod,
                uniqueId,
                coreModule.getModule().getClass()
        );
        if (null == method) {
            logger.warn("path={} is not matched any method in module {}", path, uniqueId);
            writer.println(JSONObject.toJSONString(RestResultUtils.failed(NOT_FOUND,"path=%s is not matched any method in module %s", path, uniqueId)));
            return;
        } else {
            logger.debug("path={} is matched method {} in module {}", path, method.getName(), uniqueId);
        }

        // 自动释放I/O资源
        final List<Closeable> autoCloseResources = coreModule.append(new ReleaseResource<List<Closeable>>(new ArrayList<Closeable>()) {
            @Override
            public void release() {
                final List<Closeable> closeables = get();
                if (CollectionUtils.isEmpty(closeables)) {
                    return;
                }
                for (final Closeable closeable : get()) {
                    if (closeable instanceof Flushable) {
                        try {
                            ((Flushable) closeable).flush();
                        } catch (Exception cause) {
                            logger.warn("path={} flush I/O occur error!", path, cause);
                        }
                    }
                    IOUtils.closeQuietly(closeable);
                }
            }
        });

        // 生成方法调用参数
        final Object[] parameterObjectArray = generateParameterObjectArray(autoCloseResources, method, req, resp);

        final boolean isAccessible = method.isAccessible();
        final ClassLoader oriThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            method.setAccessible(true);
            Thread.currentThread().setContextClassLoader(coreModule.getLoader());
            method.invoke(coreModule.getModule(), parameterObjectArray);
            logger.debug("path={} invoke module {} method {} success.", path, uniqueId, method.getName());
        } catch (IllegalAccessException iae) {
            logger.warn("path={} invoke module {} method {} occur access denied.", path, uniqueId, method.getName(), iae);
            throw new ServletException(iae);
        } catch (InvocationTargetException ite) {
            logger.warn("path={} invoke module {} method {} occur error.", path, uniqueId, method.getName(), ite.getTargetException());
            final Throwable targetCause = ite.getTargetException();
            if (targetCause instanceof ServletException) {
                throw (ServletException) targetCause;
            }
            if (targetCause instanceof IOException) {
                throw (IOException) targetCause;
            }
            throw new ServletException(targetCause);
        } finally {
            Thread.currentThread().setContextClassLoader(oriThreadContextClassLoader);
            method.setAccessible(isAccessible);
            coreModule.release(autoCloseResources);
        }

    }


    /**
     * 提取模块ID
     * 模块ID应该在PATH的第一个位置
     *
     * @param path servlet访问路径
     * @return 路径解析成功则返回模块的ID，如果解析失败则返回null
     */
    private String parseUniqueId(final String path) {
        final String[] pathSegmentArray = StringUtils.split(path, "/");
        return ArrayUtils.getLength(pathSegmentArray) >= 1
                ? pathSegmentArray[0]
                : null;
    }


    /**
     * 匹配模块中复合HTTP请求路径的方法
     * 匹配方法的方式是：HttpMethod和HttpPath全匹配
     *
     * @param path          HTTP请求路径
     * @param httpMethod    HTTP请求方法
     * @param uniqueId      模块ID
     * @param classOfModule 模块类
     * @return 返回匹配上的方法，如果没有找到匹配方法则返回null
     */
    private Method matchingModuleMethod(final String path,
                                        final Command.Method httpMethod,
                                        final String uniqueId,
                                        final Class<?> classOfModule) {

        // 查找@Command注解的方法
        for (final Method method : MethodUtils.getMethodsListWithAnnotation(classOfModule, Command.class)) {
            final Command commandAnnotation = method.getAnnotation(Command.class);
            if (null == commandAnnotation) {
                continue;
            }
            // 兼容 value 是否以 / 开头的写法
            String cmd = appendSlash(commandAnnotation.value());
            final String pathOfCmd = "/" + uniqueId + cmd;
            if (StringUtils.equals(path, pathOfCmd)) {
                return method;
            }
        }
        // 查找@Http注解的方法
        for (final Method method : MethodUtils.getMethodsListWithAnnotation(classOfModule, Command.class)) {
            final Command httpAnnotation = method.getAnnotation(Command.class);
            if (null == httpAnnotation) {
                continue;
            }
            // 兼容 value 是否以 / 开头的写法
            String cmd = appendSlash(httpAnnotation.value());
            final String pathPattern = "/" + uniqueId + cmd;
            if (ArrayUtils.contains(httpAnnotation.method(), httpMethod)
                    && matching(path, pathPattern)) {
                return method;
            }
        }
        // 找不到匹配方法，返回null
        return null;
    }

    private String appendSlash(String cmd) {
        // 若不以 / 开头，则添加 /
        if (!cmd.startsWith(SLASH)) {
            cmd = SLASH + cmd;
        }
        return cmd;
    }

    private boolean isMapWithGenericParameterTypes(final Method method,
                                                   final int parameterIndex,
                                                   final Class<?> keyClass,
                                                   final Class<?> valueClass) {
        final Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length < parameterIndex
                || !(genericParameterTypes[parameterIndex] instanceof ParameterizedType)) {
            return false;
        }
        final Type[] actualTypeArguments = ((ParameterizedType) genericParameterTypes[parameterIndex]).getActualTypeArguments();
        return actualTypeArguments.length == 2
                && keyClass.equals(actualTypeArguments[0])
                && valueClass.equals(actualTypeArguments[1]);
    }

    /**
     * 生成方法请求参数数组
     * 主要用于填充HttpServletRequest和HttpServletResponse
     *
     * @param autoCloseResources 自动关闭资源
     * @param method             模块Java方法
     * @param req                HttpServletRequest
     * @param resp               HttpServletResponse
     * @return 请求方法参数列表
     */
    private Object[] generateParameterObjectArray(final List<Closeable> autoCloseResources,
                                                  final Method method,
                                                  final HttpServletRequest req,
                                                  final HttpServletResponse resp) throws IOException {

        final Class<?>[] parameterTypeArray = method.getParameterTypes();
        if (ArrayUtils.isEmpty(parameterTypeArray)) {
            return null;
        }
        final Object[] parameterObjectArray = new Object[parameterTypeArray.length];
        for (int index = 0; index < parameterObjectArray.length; index++) {
            final Class<?> parameterType = parameterTypeArray[index];

            // HttpServletRequest
            if (HttpServletRequest.class.isAssignableFrom(parameterType)) {
                parameterObjectArray[index] = req;
            }

            // HttpServletResponse
            else if (HttpServletResponse.class.isAssignableFrom(parameterType)) {
                parameterObjectArray[index] = resp;
            }

            // ParameterMap<String,String[]>
            else if (Map.class.isAssignableFrom(parameterType)
                    && isMapWithGenericParameterTypes(method, index, String.class, String[].class)) {
                parameterObjectArray[index] = req.getParameterMap();
            }

            // ParameterMap<String,String>
            else if (Map.class.isAssignableFrom(parameterType)
                    && isMapWithGenericParameterTypes(method, index, String.class, String.class)) {
                final Map<String, String> param = new HashMap<String, String>();
                for (final Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    param.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
                }
                parameterObjectArray[index] = param;
            }

            // QueryString
            else if (String.class.isAssignableFrom(parameterType)) {
                parameterObjectArray[index] = req.getQueryString();
            }


            // PrintWriter
            else if (PrintWriter.class.isAssignableFrom(parameterType)) {
                final PrintWriter writer = resp.getWriter();
                autoCloseResources.add(writer);
                parameterObjectArray[index] = writer;
            }

            // OutputStream
            else if (OutputStream.class.isAssignableFrom(parameterType)) {
                final OutputStream output = resp.getOutputStream();
                autoCloseResources.add(output);
                parameterObjectArray[index] = output;
            }


        }

        return parameterObjectArray;
    }

}
