package com.hmdp.service.impl;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BloomFilterManager;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.ILock;
import com.hmdp.utils.RedisConstants;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private CacheClient cacheClient;

    @Autowired
    private BloomFilterManager bloomFilterManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Shop queryById(Long id) {
        RBloomFilter<Long> bloomFilter = bloomFilterManager.getShopBloomFilter();

        return cacheClient.queryWithBloomFilterAndProtection(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                id,
                Shop.class,
                super::getById, // 使用super关键字调用父类方法
                bloomFilter
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateShopById(Shop shop) {
        //先修改数据库
        super.updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
    }

}