package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    @Override
    public Shop queryById(Long id) {
        // 确保ID被正确转换为字符串用于Redis键
        String idStr = String.valueOf(id);
        
        //1.从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + idStr);
        //2.判断是否存在
        //3.存在直接返回
        if (shopJson != null) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //4.不存在，根据id查询数据库
        Shop shop = super.getById(id);
        //5.数据库不存在，插入空值并返回
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + idStr, "", RedisConstants.CACHE_NULL_TTL, java.util.concurrent.TimeUnit.MINUTES);
        }

        //6.存在，写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + idStr, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, java.util.concurrent.TimeUnit.MINUTES);

        //7.返回
        return shop;
    }
}
