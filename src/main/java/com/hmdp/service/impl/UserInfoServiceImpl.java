package com.hmdp.service.impl;

import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BloomFilterManager;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.ILock;
import com.hmdp.utils.RedisConstants;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
    private CacheClient cacheClient;
    @Autowired
    private BloomFilterManager bloomFilterManager;

    /**
     * 根据ID查询用户详情，使用布隆过滤器优化
     */
    public UserInfo queryUserInfoById(Long id) {
        RBloomFilter<Long> bloomFilter = bloomFilterManager.getUserInfoBloomFilter();

        return cacheClient.queryWithBloomFilterAndProtection(
                RedisConstants.CACHE_USER_INFO_KEY,
                RedisConstants.LOCK_USER_INFO_KEY,
                id,
                UserInfo.class,
                super::getById,
                bloomFilter
        );
    }

}