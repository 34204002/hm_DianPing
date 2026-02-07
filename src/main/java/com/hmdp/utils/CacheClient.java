package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;
    private final ILock ilock;
    private final BloomFilterManager bloomFilterManager;

    public CacheClient(StringRedisTemplate stringRedisTemplate, ILock ilock, BloomFilterManager bloomFilterManager) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.ilock = ilock;
        this.bloomFilterManager = bloomFilterManager;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(java.time.LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;
    }

    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.不存在，直接返回
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        java.time.LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if(expireTime.isAfter(java.time.LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return r;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = ilock.tryLock(lockKey, RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        // 6.2.判断是否获取锁成功
        if (isLock){
            // 6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R newR = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLogicalExpire(key, newR, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    ilock.unlock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return r;
    }

    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = ilock.tryLock(lockKey, RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
            // 4.2.判断是否获取成功
            if (!isLock) {
                // 4.3.获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.获取锁成功，根据id查询数据库
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6.存在，写入redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.释放锁
            ilock.unlock(lockKey);
        }
        // 8.返回
        return r;
    }

    /**
     * 综合缓存策略：使用布隆过滤器 + 互斥锁 + 空值缓存 + 随机TTL
     * 适用于单个实体查询场景，可防止缓存穿透、击穿、雪崩
     */
    public <R, ID> R queryWithBloomFilterAndProtection(
            String keyPrefix, String lockPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, RBloomFilter<ID> bloomFilter) {
        String idStr = String.valueOf(id);
        String key = keyPrefix + idStr;

        // 1. 先通过布隆过滤器判断ID是否存在（快速排除不存在的ID）
        if (!bloomFilter.contains(id)) {
            return null;
        }

        // 2.从redis中查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 3.判断是否存在
        if (json != null) {
            // 4.存在直接返回
            return JSONUtil.toBean(json, type);
        }

        // 5.缓存未命中，查询数据库
        String lockKey = lockPrefix + idStr;
        R r = null;
        try {
            //获取互斥锁
            if (!ilock.tryLock(lockKey, RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS)) {
                String jsonStr = stringRedisTemplate.opsForValue().get(key);
                if(jsonStr != null) {
                    return JSONUtil.toBean(jsonStr, type);
                }
                Thread.sleep(50);
                return queryWithBloomFilterAndProtection(keyPrefix, lockPrefix, id, type, dbFallback, bloomFilter);
            }

            // 双重检查，再次查询缓存
            json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return JSONUtil.toBean(json, type);
            }
            if (json != null && json.isEmpty()) {
                return null;
            }

            r = dbFallback.apply(id);
            // 6.数据库不存在，插入空值到缓存并返回
            if (r == null) {
                // 将空值写入缓存，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", getCacheNullTtlWithRandomness(), TimeUnit.MINUTES);
                return null;
            }

            // 7.存在，写入redis，使用随机TTL防止缓存雪崩
            String typeKey = getTypeBasedOnClass(type);
            Long ttl = getTtlWithRandomness(typeKey);
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(r), ttl, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            ilock.unlock(lockKey);
        }

        // 8.将ID加入布隆过滤器
        bloomFilter.add(id);
        // 9.返回
        return r;
    }

    /**
     * 获取基于类型的随机TTL
     */
    private Long getTtlWithRandomness(String typeKey) {
        switch (typeKey) {
            case "shop":
                return RedisConstants.getCacheShopTtlWithRandomness();
            case "user":
                return RedisConstants.getCacheUserTtlWithRandomness();
            case "userinfo":
                return RedisConstants.getCacheUserInfoTtlWithRandomness();
            default:
                return RedisConstants.CACHE_SHOP_TTL; // 默认使用商铺TTL
        }
    }

    /**
     * 根据类型获取对应的标识符
     */
    private String getTypeBasedOnClass(Class<?> type) {
        String typeName = type.getSimpleName().toLowerCase();
        if (typeName.contains("shop")) {
            return "shop";
        } else if (typeName.contains("user")) {
            if (typeName.contains("info")) {
                return "userinfo";
            } else {
                return "user";
            }
        }
        return "shop"; // 默认返回shop
    }

    private static final java.util.concurrent.ExecutorService CACHE_REBUILD_EXECUTOR = java.util.concurrent.Executors.newFixedThreadPool(10);
}