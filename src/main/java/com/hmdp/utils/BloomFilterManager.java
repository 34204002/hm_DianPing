package com.hmdp.utils;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BloomFilterManager {

    @Autowired
    private RedissonClient redissonClient;

    // 缓存已经创建的布隆过滤器，避免重复创建
    private final java.util.Map<String, RBloomFilter<Long>> bloomFilterCache = 
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 获取指定名称的布隆过滤器
     */
    public RBloomFilter<Long> getBloomFilter(String name, long expectedInsertions, double falseProbability) {
        return bloomFilterCache.computeIfAbsent(name, key -> {
            RBloomFilter<Long> filter = redissonClient.getBloomFilter(key);
            filter.tryInit(expectedInsertions, falseProbability);
            return filter;
        });
    }

    /**
     * 获取默认配置的商铺布隆过滤器
     */
    public RBloomFilter<Long> getShopBloomFilter() {
        return getBloomFilter("shopIdBloomFilter", 1_000_000, 0.03);
    }

    /**
     * 获取默认配置的用户布隆过滤器
     */
    public RBloomFilter<Long> getUserBloomFilter() {
        return getBloomFilter("userIdBloomFilter", 1_000_000, 0.03);
    }

    /**
     * 获取默认配置的用户详情布隆过滤器
     */
    public RBloomFilter<Long> getUserInfoBloomFilter() {
        return getBloomFilter("userInfoIdBloomFilter", 1_000_000, 0.03);
    }

    /**
     * 获取默认配置的博客布隆过滤器
     */
    public RBloomFilter<Long> getBlogBloomFilter() {
        return getBloomFilter("blogIdBloomFilter", 1_000_000, 0.03);
    }

    /**
     * 获取默认配置的优惠券布隆过滤器
     */
    public RBloomFilter<Long> getVoucherBloomFilter() {
        return getBloomFilter("voucherIdBloomFilter", 1_000_000, 0.03);
    }

    /**
     * 获取默认配置的关注布隆过滤器
     */
    public RBloomFilter<Long> getFollowBloomFilter() {
        return getBloomFilter("followIdBloomFilter", 1_000_000, 0.03);
    }
}
