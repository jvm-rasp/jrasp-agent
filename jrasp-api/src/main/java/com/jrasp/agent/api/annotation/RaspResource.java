package com.jrasp.agent.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RASP module resource inject
 * 1.避免与 javax.annotation.* 中的注解冲突
 * 2.仅限于字段上面加上注解
 * @author jrasp
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RaspResource {

}
