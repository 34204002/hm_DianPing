package com.hmdp.utils;

import java.util.Random;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 10800L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    
    public static final String CACHE_USER_KEY = "cache:user:";
    public static final Long CACHE_USER_TTL = 30L;
    
    public static final String CACHE_USER_INFO_KEY = "cache:userinfo:";
    public static final Long CACHE_USER_INFO_TTL = 30L;

    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop:type:";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
    
    public static final String LOCK_USER_KEY = "lock:user:";
    public static final String LOCK_USER_INFO_KEY = "lock:userinfo:";

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";

    private static final Random random = new Random();

    // 为TTL添加随机范围的方法，防止缓存雪崩
    public static long getCacheShopTtlWithRandomness() {
        // 在基础TTL上增加0-5分钟的随机时间
        return CACHE_SHOP_TTL + random.nextInt(6);
    }

    public static long getCacheUserTtlWithRandomness() {
        // 在基础TTL上增加0-5分钟的随机时间
        return CACHE_USER_TTL + random.nextInt(6);
    }

    public static long getCacheUserInfoTtlWithRandomness() {
        // 在基础TTL上增加0-5分钟的随机时间
        return CACHE_USER_INFO_TTL + random.nextInt(6);
    }

    public static long getCacheShopTypeTtlWithRandomness() {
        // 在基础TTL上增加0-5分钟的随机时间
        return CACHE_SHOP_TYPE_TTL + random.nextInt(6);
    }

    public static long getLoginUserTtlWithRandomness() {
        // 在基础TTL上增加0-10分钟的随机时间
        return LOGIN_USER_TTL + random.nextInt(11);
    }

    public static long getCacheNullTtlWithRandomness() {
        // 在基础TTL上增加0-2分钟的随机时间
        return CACHE_NULL_TTL + random.nextInt(3);
    }
}