package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.*;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务实现类
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

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {

        //判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按类型查询
            //根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        //查询redis、按照距离排序、分页
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().limit(end)
        );
        //获取店铺id
        if (results == null||results.getContent().size() <= from) {
            return Result.ok();
        }
        List<Long> ids = results.getContent().stream()
                .skip(from)
                .map(result->result.getContent().getName())
                .map(result->Long.valueOf(String.valueOf(result)))
                .toList();
        List<Distance> distances = results.getContent().stream()
                .skip(from)
                .map(GeoResult::getDistance)
                .toList();
        //获取店铺信息
        String idsStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id, " + idsStr + ")").list();
        for (int i = 0; i < shops.size(); i++) {
            Shop shop = shops.get(i);
            shop.setDistance(distances.get(i).getValue());
        }
        return Result.ok(shops);
    }

}