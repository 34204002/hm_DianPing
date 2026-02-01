package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BloomFilterManager;
import com.hmdp.utils.RedisConstants;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

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
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private BloomFilterManager bloomFilterManager;

    @Override
    public Shop queryById(Long id) {
        // 确保ID被正确转换为字符串用于Redis键
        String idStr = String.valueOf(id);
        
        // 1. 先通过布隆过滤器判断ID是否存在（快速排除不存在的ID）
        RBloomFilter<Long> bloomFilter = bloomFilterManager.getShopBloomFilter();
        if (!bloomFilter.contains(id)) {
            // 如果布隆过滤器认为不存在，直接返回null，避免访问缓存和数据库
            return null;
        }

        // 2.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + idStr);
        // 3.判断是否存在
        // 4.存在直接返回
        if (shopJson != null) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 5.缓存未命中，查询数据库
        Shop shop = super.getById(id);
        // 6.数据库不存在，插入空值到缓存并返回
        if (shop == null) {
            // 将空值写入缓存，防止缓存穿透
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + idStr, "", RedisConstants.CACHE_NULL_TTL, java.util.concurrent.TimeUnit.MINUTES);
            return null;
        }

        // 7.存在，写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + idStr, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, java.util.concurrent.TimeUnit.MINUTES);
        // 8.将ID加入布隆过滤器
        bloomFilter.add(id);
        // 9.返回
        return shop;
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
