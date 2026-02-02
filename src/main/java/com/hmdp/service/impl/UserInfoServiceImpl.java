package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
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

        // 2.从redis中查询用户详情缓存
        String userInfoJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_USER_INFO_KEY + idStr);
        // 3.判断是否存在
        // 4.存在直接返回
        if (userInfoJson != null) {
            return cn.hutool.json.JSONUtil.toBean(userInfoJson, UserInfo.class);
        }
        // 5.缓存未命中，查询数据库
        String key = RedisConstants.LOCK_USER_INFO_KEY + idStr;
        UserInfo userInfo = null;
        try {
            //获取互斥锁
            if (!tryLock(key)) {
                String userInfoJsonStr = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_USER_INFO_KEY + idStr);
                if(userInfoJsonStr != null)
                    return cn.hutool.json.JSONUtil.toBean(userInfoJsonStr, UserInfo.class);
                Thread.sleep(50);
                return queryUserInfoById(id);
            }

            userInfo = super.getById(id);
            // 6.数据库不存在，插入空值到缓存并返回
            if (userInfo == null) {
                // 将空值写入缓存，防止缓存穿透
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_USER_INFO_KEY + idStr, "", RedisConstants.getCacheNullTtlWithRandomness(), java.util.concurrent.TimeUnit.MINUTES);
                return null;
            }

            // 7.存在，写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_USER_INFO_KEY + idStr, cn.hutool.json.JSONUtil.toJsonStr(userInfo), RedisConstants.getCacheUserInfoTtlWithRandomness(), java.util.concurrent.TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unlock(key);
        }

        // 8.将ID加入布隆过滤器
        bloomFilter.add(id);
        // 9.返回
        return userInfo;
    }
    
    public boolean tryLock(String key){
        Boolean flag =stringRedisTemplate.opsForValue().setIfAbsent(key, "1");
        return BooleanUtil.isTrue(flag);
    }
    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
