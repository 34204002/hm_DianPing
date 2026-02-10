package com.hmdp.utils;


import cn.hutool.core.util.BooleanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式互斥锁工具类
 * 用于统一管理Redis分布式锁的获取和释放操作
 */
@Slf4j
@Component
public class RedisLockUtils implements ILock {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String UUIDStr = UUID.randomUUID().toString();
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    public RedisLockUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取分布式锁
     *
     * @param key      锁的键
     * @param timeout  锁的超时时间
     * @param unit     时间单位
     * @return 是否获取锁成功
     */
    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(
                key,
                UUIDStr+"-"+Thread.currentThread().threadId(),
                timeout,
                unit
        );
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 尝试获取分布式锁，默认超时时间为10秒
     *
     * @param key 锁的键
     * @return 是否获取锁成功
     */
    @Override
    public boolean tryLock(String key) {
        return tryLock(key, 10, TimeUnit.SECONDS);
    }

    /**
     * 释放分布式锁
     *
     * @param key 锁的键
     */
    @Override
    public void unlock(String key) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), UUIDStr+"-"+Thread.currentThread().threadId());

    }
//    @Override
//    public void unlock(String key) {
//        String value = stringRedisTemplate.opsForValue().get(key);
//        if(value!=null&&value.equals(UUIDStr+"-"+Thread.currentThread().threadId())) {
//            stringRedisTemplate.delete(key);
//        }
//
//    }
}