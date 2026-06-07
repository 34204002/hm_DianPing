package com.hmdp.aop;

import cn.hutool.core.util.StrUtil;
import com.hmdp.annotation.RateLimiter;
import com.hmdp.dto.Result;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分布式限流AOP切面，基于Redisson令牌桶算法
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(rateLimiter)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        String key = resolveKey(rateLimiter.key(), joinPoint);
        String redisKey = RedisConstants.RATE_LIMIT_KEY + key;

        RRateLimiter limiter = redissonClient.getRateLimiter(redisKey);
        limiter.trySetRate(RateType.OVERALL, rateLimiter.permits(), rateLimiter.period(), RateIntervalUnit.SECONDS);

        if (limiter.tryAcquire()) {
            return joinPoint.proceed();
        }

        log.warn("令牌桶限流触发: key={}, permits={}, period={}s", key, rateLimiter.permits(), rateLimiter.period());
        return Result.fail(rateLimiter.message());
    }

    private String resolveKey(String keyExpr, ProceedingJoinPoint joinPoint) {
        if (StrUtil.isNotBlank(keyExpr)) {
            return keyExpr;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }
}
