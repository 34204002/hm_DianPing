package com.hmdp.service.impl;

import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BloomFilterManager;
import com.hmdp.utils.RedisConstants;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BloomFilterManager bloomFilterManager;


    /**
     * 根据ID查询用户详情，使用布隆过滤器优化
     */
    public UserInfo queryUserInfoById(Long id) {
        // 确保ID被正确转换为字符串用于Redis键
        String idStr = String.valueOf(id);
        
        // 1. 先通过布隆过滤器判断ID是否存在（快速排除不存在的ID）
        RBloomFilter<Long> bloomFilter = bloomFilterManager.getUserInfoBloomFilter();
        if (!bloomFilter.contains(id)) {
            // 如果布隆过滤器认为不存在，直接返回null，避免访问缓存和数据库
            return null;
        }

        // 2. 从redis中查询用户详情缓存
        String userInfoJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_USER_INFO_KEY + idStr);
        // 3. 判断缓存是否存在
        if (userInfoJson != null) {
            // 3.1 存在且不是空值，直接返回
            if (!"".equals(userInfoJson)) {
                return cn.hutool.json.JSONUtil.toBean(userInfoJson, UserInfo.class);
            }
            // 3.2 存在但为空值，表示数据库中不存在该用户详情
            return null;
        }

        // 4. 缓存未命中，查询数据库
        UserInfo userInfo = getById(id);
        
        // 5. 数据库不存在，插入空值到缓存并返回
        if (userInfo == null) {
            // 将空值写入缓存，防止缓存穿透
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_USER_INFO_KEY + idStr, "", RedisConstants.getCacheNullTtlWithRandomness(), java.util.concurrent.TimeUnit.MINUTES);
            return null;
        }

        // 6. 存在，写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_USER_INFO_KEY + idStr, cn.hutool.json.JSONUtil.toJsonStr(userInfo), RedisConstants.getCacheUserInfoTtlWithRandomness(), java.util.concurrent.TimeUnit.MINUTES);
        // 7. 将ID加入布隆过滤器
        bloomFilter.add(id);
        
        return userInfo;
    }
}
