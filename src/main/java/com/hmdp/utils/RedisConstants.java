package com.hmdp.utils;

import java.util.Random;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 14400L;

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
    public static final String LOCK_ORDER_USER_KEY = "lock:order:";
    public static final Long LOCK_ORDER_USER_TTL = 10L;
    
    public static final String LOCK_USER_KEY = "lock:user:";
    public static final String LOCK_USER_INFO_KEY = "lock:userinfo:";

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_VOUCHER_USER_KEY = "seckill:order:";
    public static final String SECKILL_VOUCHER_TIME_KEY = "seckill:time:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String SHOP_UV_KEY = "uv:shop:";
    public static final String NEO4J_RECOMMEND_FRIENDS_VISITED = "neo4j:recommend:friends:visited:";
    public static final String NEO4J_RECOMMEND_FRIENDS_LIKED = "neo4j:recommend:friends:liked:";
    public static final String NEO4J_RECOMMEND_MIGHT_KNOW = "neo4j:recommend:mightknow:";
    public static final String AI_SEARCH_KEY = "ai:search:";
    public static final String DAU_KEY = "dau:";
    public static final Long DAU_KEY_TTL_DAYS = 90L;
    public static final String WAU_KEY = "wau:";
    public static final Long WAU_KEY_TTL_DAYS = 120L;
    public static final String MAU_KEY = "mau:";
    public static final Long MAU_KEY_TTL_DAYS = 365L;
    public static final String HOT_SHOP_KEY = "hot:shop:";
    public static final Long HOT_SHOP_KEY_TTL_DAYS = 180L;
    public static final String USER_BEHAVIOR_KEY = "user:behavior:";
    public static final Long USER_BEHAVIOR_KEY_TTL_DAYS = 730L;
    public static final String USER_BEHAVIOR_ALL_KEY = "user:behavior:all:";
    public static final String RATE_LIMIT_KEY = "rate:limit:";


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