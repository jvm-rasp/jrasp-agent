package com.jrasp.agent.module.expression.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * 表达式注入: spel、ognl、mvel、el、jexl2、jexl3
 * 覆盖的中间件比较全面
 *
 * @author jrasp
 */
@MetaInfServices(Module.class)
@Information(id = "expression-hook")
public class ExpressionHook implements Module, LoadCompleted {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    private volatile Boolean disable = false;

    @Override
    public void loadCompleted() {
        hook();
    }

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = ParamSupported.getParameter(configMaps, "disable", Boolean.class, false);
        return true;
    }

    /**
     * @see org.springframework.expression.common.TemplateAwareExpressionParser#parseExpression
     */
    public void hook() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("org/springframework/expression/common/TemplateAwareExpressionParser")
                        .onMethod("parseExpression(Ljava/lang/String;Lorg/springframework/expression/ParserContext;)Lorg/springframework/expression/Expression;",
                                new AdviceListener() {
                                    @Override
                                    protected void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        String expressionString = (String) advice.getParameterArray()[0];
                                        algorithmManager.doCheck("spel", context.get(), expressionString);
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                )
                /**
                 * @see ognl.OgnlParser#topLevelExpression
                 * @see ognl.DefaultMemberAccess
                 */
                .onClass(new ClassMatcher("ognl/OgnlParser")
                        .onMethod("topLevelExpression()Lognl/Node;", new AdviceListener() {
                            @Override
                            protected void afterReturning(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                Object object = advice.getReturnObj();
                                if (object != null) {
                                    String expression = String.valueOf(object);
                                    algorithmManager.doCheck("ognl", context.get(), expression);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )
                /**
                 * @see org.apache.el.ValueExpressionImpl#getValue
                 */
                .onClass(new ClassMatcher("org/apache/el/ValueExpressionImpl")
                        .onMethod(new String[]{
                                "getValue(Ljakarta/el/ELContext;)Ljava/lang/Object;",
                                "getValue(Ljavax/el/ELContext;)Ljava/lang/Object;"}, new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                org.apache.el.ValueExpressionImpl expression = (org.apache.el.ValueExpressionImpl) advice.getTarget();
                                if (expression != null) {
                                    String expressionString = expression.getExpressionString();
                                    algorithmManager.doCheck("el", context.get(), expressionString);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )
                /**
                 * @see org.apache.commons.jexl2.ExpressionImpl#execute
                 */
                .onClass(new ClassMatcher("org/apache/commons/jexl2/ExpressionImpl")
                        .onMethod(new String[]{
                                "execute(Lorg/apache/commons/jexl2/JexlContext;[Ljava/lang/Object;)Ljava/lang/Object;",
                                "execute(Lorg/apache/commons/jexl2/JexlContext;)Ljava/lang/Object;"
                        }, new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                org.apache.commons.jexl2.ExpressionImpl expression = (org.apache.commons.jexl2.ExpressionImpl) advice.getTarget();
                                if (expression != null) {
                                    String expressionString = expression.getExpression();
                                    algorithmManager.doCheck("jexl", context.get(), expressionString);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )
                /**
                 * @see org.apache.commons.jexl3.internal.Script#execute
                 */
                .onClass(new ClassMatcher("org/apache/commons/jexl3/internal/Script")
                        .onMethod(new String[]{
                                "execute(Lorg/apache/commons/jexl3/JexlContext;)Ljava/lang/Object;",
                                "execute(Lorg/apache/commons/jexl3/JexlContext;[Ljava/lang/Object;)Ljava/lang/Object;"
                        }, new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                org.apache.commons.jexl3.internal.Script expression = (org.apache.commons.jexl3.internal.Script) advice.getTarget();
                                if (expression != null) {
                                    String expressionString = expression.getSourceText();
                                    algorithmManager.doCheck("jexl", context.get(), expressionString);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                )
                /**
                 * @see org.mvel2.MVEL#compileExpression(char[], ParserContext)  最终调用
                 * @see org.mvel2.MVEL#compileExpression(String, ParserContext)  最终调用
                 * @see org.mvel2.MVEL#compileExpression(char[], int, int, ParserContext) 最终调用
                 * compileExpression([CLorg/mvel2/ParserContext;)Ljava/io/Serializable;
                 * compileExpression(Ljava/lang/String;Lorg/mvel2/ParserContext;)Ljava/io/Serializable;
                 * compileExpression([CIILorg/mvel2/ParserContext;)Ljava/io/Serializable;
                 */
                .onClass(new ClassMatcher("org/mvel2/MVEL")
                        .onMethod(new String[]{
                                "compileExpression(Ljava/lang/String;Lorg/mvel2/ParserContext;)Ljava/io/Serializable;",
                                "eval(Ljava/lang/String;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Lorg/mvel2/integration/VariableResolverFactory;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Object;Lorg/mvel2/integration/VariableResolverFactory;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/util/Map;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Lorg/mvel2/integration/VariableResolverFactory;Ljava/lang/Class;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/util/Map;Ljava/lang/Class;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Object;Lorg/mvel2/integration/VariableResolverFactory;Ljava/lang/Class;)Ljava/lang/Object;",
                                "eval(Ljava/lang/String;Ljava/lang/Object;Ljava/util/Map;Ljava/lang/Class;)Ljava/lang/Object;"
                        }, new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                String expression = (String) advice.getParameterArray()[0];
                                if (expression != null) {
                                    algorithmManager.doCheck("mvel", context.get(), expression);
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                        .onMethod(new String[]{
                                        "compileExpression([CLorg/mvel2/ParserContext;)Ljava/io/Serializable;",
                                        "eval([C)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Object;)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Class;)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Object;Lorg/mvel2/integration/VariableResolverFactory;)Ljava/lang/Object;",
                                        "eval([CIILjava/lang/Object;Lorg/mvel2/integration/VariableResolverFactory;)Ljava/lang/Object;",
                                        "eval([CIILjava/lang/Object;Lorg/mvel2/integration/VariableResolverFactory;Ljava/lang/Class;)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Object;Ljava/util/Map;Ljava/lang/Class;)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;",
                                        "eval([CLjava/lang/Object;Lorg/mvel2/integration/VariableResolverFactory;Ljava/lang/Class;)Ljava/lang/Object;",
                                        "eval([CLorg/mvel2/integration/VariableResolverFactory;Ljava/lang/Class;)Ljava/lang/Object;",
                                        "eval([CLjava/util/Map;Ljava/lang/Class;)Ljava/lang/Object;"
                                },
                                new AdviceListener() {
                                    @Override
                                    protected void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }
                                        char[] expression = (char[]) advice.getParameterArray()[0];
                                        if (expression != null) {
                                            algorithmManager.doCheck("mvel", context.get(), new String(expression));
                                        }
                                    }

                                    @Override
                                    protected void afterThrowing(Advice advice) throws Throwable {
                                        context.remove();
                                    }
                                })
                        .onMethod("compileExpression([CIILorg/mvel2/ParserContext;)Ljava/io/Serializable;", new AdviceListener() {
                            @Override
                            protected void before(Advice advice) throws Throwable {
                                if (disable) {
                                    return;
                                }
                                char[] expression = (char[]) advice.getParameterArray()[0];
                                int start = (Integer) advice.getParameterArray()[1];
                                int offset = (Integer) advice.getParameterArray()[2];
                                if (expression != null) {
                                    algorithmManager.doCheck("mvel", context.get(), new String(expression, start, offset));
                                }
                            }

                            @Override
                            protected void afterThrowing(Advice advice) throws Throwable {
                                context.remove();
                            }
                        })
                ).onClass(new ClassMatcher("org/primefaces/application/StreamedContentHandler")
                        .onMethod("handle(Ljavax/faces/context/FacesContext;)V"
                                , new AdviceListener() {
                                    @Override
                                    protected void before(Advice advice) throws Throwable {
                                        if (disable) {
                                            return;
                                        }

                                        Object facesContext = advice.getParameterArray()[0];
                                        algorithmManager.doCheck("primefaces", context.get(), facesContext);
                                    }
                                })
                ).build();
    }
}
