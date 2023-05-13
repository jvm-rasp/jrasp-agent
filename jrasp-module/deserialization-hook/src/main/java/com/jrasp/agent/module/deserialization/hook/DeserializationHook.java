package com.jrasp.agent.module.deserialization.hook;

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
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.io.ObjectStreamClass;
import java.net.URLDecoder;
import java.util.Map;

/**
 * 反序列化： jdk反序列化、fastjson反序列化、jackson反序列化、xstream反序列化、yaml反序列化
 *
 * @author jrasp
 */
@MetaInfServices(Module.class)
@Information(id = "deserialization-hook", author = "jrasp")
public class DeserializationHook extends ModuleLifecycleAdapter implements Module, LoadCompleted {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private String metaInfo;

    /**
     * object Input stream
     */
    private static final String OIS_TYPE = "ois-deserialization";

    private static final String JSON_YAML_TYPE = "json-yaml-deserialization";

    private static final String XML_TYPE = "xml-deserialization";

    private static final String YAML_TAG = "tag:yaml.org,2002:";

    private volatile Boolean disable = false;

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, false);
        return true;
    }

    @Override
    public void loadCompleted() {
        deserializationClassRceHook();
    }

    public void deserializationClassRceHook() {
        new EventWatchBuilder(moduleEventWatcher)
                /**
                 * @see java.io.ObjectInputStream#resolveClass(java.io.ObjectStreamClass)
                 */
                .onClass(new ClassMatcher("java/io/ObjectInputStream")
                        .onMethod(
                                "resolveClass(Ljava/io/ObjectStreamClass;)Ljava/lang/Class;", new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        ObjectStreamClass objectStreamClass = (ObjectStreamClass) advice.getParameterArray()[0];
                                        if (objectStreamClass != null) {
                                            algorithmManager.doCheck(OIS_TYPE, context.get(), objectStreamClass.getName());
                                        }
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                )
                /**
                 * fastjson
                 * @see com.alibaba.fastjson.util.TypeUtils#loadClass(String, ClassLoader, boolean)
                 * @see com.alibaba.fastjson.util.TypeUtils#getClassFromMapping(String)
                 */
                .onClass(new ClassMatcher("com/alibaba/fastjson/util/TypeUtils")
                        .onMethod(
                                new String[]{
                                        "loadClass(Ljava/lang/String;Ljava/lang/ClassLoader;Z)Ljava/lang/Class;",
                                        "getClassFromMapping(Ljava/lang/String;)Ljava/lang/Class;"
                                }, new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        String clazzName = (String) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(JSON_YAML_TYPE, context.get(), clazzName);
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                )
                /**
                 * jackson
                 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer#deserialize
                 */
                .onClass(new ClassMatcher("com/fasterxml/jackson/databind/deser/BeanDeserializer")
                        .onMethod(new String[]{
                                        "deserialize(Lcom/fasterxml/jackson/core/JsonParser;Lcom/fasterxml/jackson/databind/DeserializationContext;)Ljava/lang/Object;",
                                        "deserialize(Lcom/fasterxml/jackson/core/JsonParser;Lcom/fasterxml/jackson/databind/DeserializationContext;Ljava/lang/Object;)Ljava/lang/Object;"
                                }
                                , new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        com.fasterxml.jackson.databind.deser.BeanDeserializer beanDeserializer = (com.fasterxml.jackson.databind.deser.BeanDeserializer) advice.getTarget();
                                        if (beanDeserializer != null) {
                                            final Class<?> aClass = beanDeserializer.handledType();
                                            String clazzName = aClass.getName();
                                            algorithmManager.doCheck(JSON_YAML_TYPE, context.get(), clazzName);
                                        }
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                )
                /**
                 * @see com.thoughtworks.xstream.core.AbstractReferenceUnmarshaller#convert
                 */
                .onClass(new ClassMatcher("com/thoughtworks/xstream/core/AbstractReferenceUnmarshaller")
                        .onMethod(
                                "convert(Ljava/lang/Object;Ljava/lang/Class;Lcom/thoughtworks/xstream/converters/Converter;)Ljava/lang/Object;", new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        ObjectStreamClass objectStreamClass = (ObjectStreamClass) advice.getParameterArray()[0];
                                        algorithmManager.doCheck(XML_TYPE, context.get(), objectStreamClass);
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                )
                /**
                 * yaml
                 * @see org.yaml.snakeyaml.constructor.BaseConstructor#constructObject
                 */
                .onClass(new ClassMatcher("org/yaml/snakeyaml/constructor/BaseConstructor")
                        .onMethod(
                                "constructObject(Lorg/yaml/snakeyaml/nodes/Node;)Ljava/lang/Object;", new AdviceListener() {
                                    @Override
                                    public void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        org.yaml.snakeyaml.nodes.Node node = (org.yaml.snakeyaml.nodes.Node) advice.getParameterArray()[0];
                                        if (null != node) {
                                            org.yaml.snakeyaml.nodes.Tag tag = node.getTag();
                                            if (tag != null) {
                                                String className = URLDecoder.decode(tag.toString().substring(YAML_TAG.length()), "UTF-8");
                                                algorithmManager.doCheck(JSON_YAML_TYPE, context.get(), className);
                                            }
                                        }
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
