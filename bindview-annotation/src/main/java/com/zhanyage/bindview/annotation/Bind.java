package com.zhanyage.bindview.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by andya on 2019/4/12
 * Describe: bind view annotation
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Bind {

    int value();
}
