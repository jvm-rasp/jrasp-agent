package com.jrasp.api.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
public @interface Command {

    String value();

    Method[] method() default {Method.GET, Method.POST};

    enum Method {
        GET,
        POST
    }

}
