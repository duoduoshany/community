package com.gongsi.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//是否需要登录的意思，这个注解需要用元注解定义
@Target(ElementType.METHOD)//自定义注解可以用的元素类型：如只能应用于方法。
@Retention(RetentionPolicy.RUNTIME)//Retention 定义了自定义注解的 生命周期，即注解信息在运行时有效。
public @interface LoginRequired {
}
