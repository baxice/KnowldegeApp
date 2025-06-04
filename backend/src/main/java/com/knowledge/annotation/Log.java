package com.knowledge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 日志记录注解
 * 用于标记需要记录操作日志的方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    
    /**
     * 操作类型
     */
    String value() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
    
    /**
     * 操作模块
     */
    String module() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean logResult() default true;
    
    /**
     * 是否记录执行时间
     */
    boolean logExecutionTime() default true;
} 