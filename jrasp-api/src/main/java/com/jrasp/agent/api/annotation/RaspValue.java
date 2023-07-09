package com.jrasp.agent.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 1.标记一个字段，这个字段的名称和值自动生成配置参数列表
 * 2.仅限于字段上面加上注解
 * @author jrasp
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RaspValue {

    String name() default "";

    String[] value() default {};
}
