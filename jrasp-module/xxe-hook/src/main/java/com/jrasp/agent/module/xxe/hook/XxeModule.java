package com.jrasp.agent.module.xxe.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
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
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

/**
 * 支持的中间件：
 * dom4j、jdom1、jdom2、xerces
 *
 * @author jrasp
 */
@MetaInfServices(Module.class)
@Information(id = "xxe-hook", author = "jrasp")
public class XxeModule implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    private volatile boolean disable = false;

    @Override
    public void loadCompleted() {
        xxeHook();
    }

    /**
     * 更新模块参数
     *
     * @param configMaps
     * @return 更新是否成功
     */
    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, disable);
        return true;
    }

    private static final String FEATURE_DEFAULTS_1 = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_DEFAULTS_2 = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_DEFAULTS_3 = "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_DEFAULTS_4 = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    public void xxeHook() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * @see org.dom4j.io.SAXReader#read(org.xml.sax.InputSource)
                 * 2022-10-26 已经验证
                 * 2022-12-07 已经验证
                 * 对open-rasp hook类进行了优化：仅hook read方法最底层的调用方法，避免了hook逻辑重复执行
                 */
                .onClass(new ClassMatcher("org/dom4j/io/SAXReader")
                        .onMethod("read(Lorg/xml/sax/InputSource;)Lorg/dom4j/Document;", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                org.dom4j.io.SAXReader saxReader = (org.dom4j.io.SAXReader) advice.getTarget();
                                saxReader.setFeature(FEATURE_DEFAULTS_1, true);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )

                /**
                 * @see org.jdom.input.SAXBuilder#build(URL)
                 * @see org.jdom.input.SAXBuilder#build(File)
                 * @see org.jdom.input.SAXBuilder#build(InputStream)
                 * @see org.jdom.input.SAXBuilder#build(Reader, String)
                 * @see org.jdom.input.SAXBuilder#build(String)
                 * @see org.jdom.input.SAXBuilder#build(InputStream, String)
                 * @see org.jdom.input.SAXBuilder#build(InputSource)   最终调用方法
                 */
                .onClass(new ClassMatcher("org/jdom/input/SAXBuilder")
                        .onMethod("build(Ljava/io/InputStream;)Lorg/jdom/Document;", new AdviceListener() {
                            @Override
                            public void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                org.jdom.input.SAXBuilder saxBuilder = (org.jdom.input.SAXBuilder) advice.getTarget();
                                saxBuilder.setFeature(FEATURE_DEFAULTS_1, true);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )

                /**
                 * @see org.jdom2.input.SAXBuilder#build(URL)
                 * @see org.jdom2.input.SAXBuilder#build(File)
                 * @see org.jdom2.input.SAXBuilder#build(InputStream)
                 * @see org.jdom2.input.SAXBuilder#build(Reader, String)
                 * @see org.jdom2.input.SAXBuilder#build(Reader)
                 * @see org.jdom2.input.SAXBuilder#build(String)
                 * @see org.jdom2.input.SAXBuilder#build(InputStream, String)
                 * @see org.jdom2.input.SAXBuilder#build(InputSource)
                 * 以上全部需要hook
                 */
                .onClass(new ClassMatcher("org/jdom2/input/SAXBuilder")
                        .onMethod(new String[]{
                                        "build(Lorg/xml/sax/InputSource;)Lorg/jdom2/Document;",
                                        "build(Ljava/io/InputStream;)Lorg/jdom2/Document;",
                                        "build(Ljava/io/File;)Lorg/jdom2/Document;",
                                        "build(Ljava/net/URL;)Lorg/jdom2/Document;",
                                        "build(Ljava/io/InputStream;Ljava/lang/String;)Lorg/jdom2/Document;",
                                        "build(Ljava/io/Reader;)Lorg/jdom2/Document;",
                                        "build(Ljava/io/Reader;Ljava/lang/String;)Lorg/jdom2/Document;",
                                        "build(Ljava/lang/String;)Lorg/jdom2/Document;"},
                                new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        org.jdom2.input.SAXBuilder saxBuilder = (org.jdom2.input.SAXBuilder) advice.getTarget();
                                        saxBuilder.setFeature(FEATURE_DEFAULTS_1, true);
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                )
                /**
                 * @see javax.xml.parsers.DocumentBuilderFactory#newInstance()
                 * wiki:https://blog.spoock.com/2018/05/16/cve-2018-1259/
                 */
                .onClass(new ClassMatcher("javax/xml/parsers/DocumentBuilderFactory")
                        .onMethod("newInstance()Ljavax/xml/parsers/DocumentBuilderFactory;", new AdviceListener() {

                            @Override
                            protected void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                javax.xml.parsers.DocumentBuilderFactory instance = (javax.xml.parsers.DocumentBuilderFactory) advice.getReturnObj();
                                // bugfix: xercesImpl2.6.2版本没有实现setFeature方法,调用会报错:
                                // java.lang.AbstractMethodError:javax.xml.parsers.DocumentBuilderFactory.setFeature
                                // 根因: 业务代码存在多个xml解析器，覆盖了jdk默认的xml解析器
                                if (instance != null) {
                                    try {
                                        instance.getClass().getDeclaredMethod("setFeature", String.class, boolean.class);
                                    } catch (Exception e) {
                                        // 找不到方法直接返回
                                        return;
                                    }
                                    // 这个是基本的防御方式。 如果DTDs被禁用, 能够防止绝大部分的XXE;
                                    // 如果这里设置为true会影响mybatis-xml的加载
                                    instance.setFeature(FEATURE_DEFAULTS_1, true);
                                    // 如果不能完全禁用DTDs，至少下面的几个需要禁用
                                    instance.setFeature(FEATURE_DEFAULTS_2, false);
                                    instance.setFeature(FEATURE_DEFAULTS_3, false);
                                    instance.setFeature(FEATURE_DEFAULTS_4, false);
                                    instance.setXIncludeAware(false);
                                    instance.setExpandEntityReferences(false);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 context 信息，防止内存泄漏
                                context.remove();
                            }
                        })
                )
                /**
                 * @see javax.xml.stream.XMLInputFactory#newInstance()
                 */
                .onClass(new ClassMatcher("javax/xml/stream/XMLInputFactory")
                        .onMethod("newInstance()Ljavax/xml/stream/XMLInputFactory;", new AdviceListener() {
                            @Override
                            protected void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                javax.xml.stream.XMLInputFactory xmlInputFactory = (javax.xml.stream.XMLInputFactory) advice.getReturnObj();
                                // This disables DTDs entirely for that factory
                                // 下面是open-rasp 的防护策略
                                xmlInputFactory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, false);
                                // 下面的2个是owasp建议，启用之后，调用会报错
                                // This causes XMLStreamException to be thrown if external DTDs are accessed.
                                // java.lang.IllegalArgumentException: Unrecognized property 'http://javax.xml.XMLConstants/property/accessExternalDTD'
                                // xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                                // disable external entities
                                // xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                // 代码执行异常情况清除 context 信息，防止内存泄漏
                                context.remove();
                            }
                        })
                )
                /**
                 * @see org.xml.sax.helpers.XMLReaderFactory#createXMLReader()
                 */
                .onClass(new ClassMatcher("org/xml/sax/helpers/XMLReaderFactory")
                        .onMethod("createXMLReader()Lorg/xml/sax/XMLReader;", new AdviceListener() {
                            @Override
                            public void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                org.xml.sax.XMLReader xmlReader = (org.xml.sax.XMLReader) advice.getReturnObj();
                                // 这个是基本的防御方式。 如果DTDs被禁用, 能够防止绝大部分的XXE;
                                xmlReader.setFeature(FEATURE_DEFAULTS_1, true); // todo
                                // 如果不能完全禁用DTDs，至少下面的几个需要禁用:(推荐)
                                xmlReader.setFeature(FEATURE_DEFAULTS_2, false);
                                xmlReader.setFeature(FEATURE_DEFAULTS_3, false);
                                xmlReader.setFeature(FEATURE_DEFAULTS_4, false);
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )
                .build();
    }
}
