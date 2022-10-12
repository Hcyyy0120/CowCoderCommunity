package com.hcy.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注哪些方法在登录以后才能访问
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)//运行时也存在
public @interface LoginRequired {


}
