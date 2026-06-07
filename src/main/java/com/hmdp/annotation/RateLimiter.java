package com.hmdp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式限流注解，基于Redisson令牌桶算法实现
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    /** 限流key前缀 */
    String key() default "";
    /** 时间窗口内允许的请求数 */
    int permits() default 10;
    /** 时间窗口大小（秒） */
    int period() default 60;
    /** 超过限制时返回的提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
