package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        //从redis查询
        List<String> cacheList = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_SHOP_TYPE_KEY, 0, -1);
        //存在，直接返回
        if (cacheList != null && !cacheList.isEmpty()) {
            log.debug("从redis中查询");
            return cacheList.stream().map(shopType -> JSONUtil.toBean(shopType, ShopType.class)).toList();
        }
        //如果不存在，数据库中查询并插入到redis
        log.debug("从数据库中查询");
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        if (shopTypeList == null) {
            return null;
        }
        // 将ShopType对象转换为JSON字符串后再存储到Redis
        List<String> jsonList = shopTypeList.stream()
                .map(shopType -> JSONUtil.toJsonStr(shopType))
                .toList();
        
        stringRedisTemplate.opsForList().leftPushAll(RedisConstants.CACHE_SHOP_TYPE_KEY,jsonList);
        // 设置过期时间为2小时
        stringRedisTemplate.expire(RedisConstants.CACHE_SHOP_TYPE_KEY, RedisConstants.CACHE_SHOP_TYPE_TTL, java.util.concurrent.TimeUnit.MINUTES);

        return shopTypeList;
    }
}
