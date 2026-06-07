package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.neo4j.KnownUserDTO;
import com.hmdp.dto.neo4j.ShopRecommendDTO;
import com.hmdp.repository.Neo4jCypherRepository;
import com.hmdp.service.INeo4jRecommendService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Neo4j社交推荐服务，基于图数据库查询好友推荐、可能认识的人等
 */
@Slf4j
@Service
public class Neo4jRecommendServiceImpl implements INeo4jRecommendService {

    private final Neo4jCypherRepository cypherRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public Neo4jRecommendServiceImpl(Neo4jCypherRepository cypherRepository,
                                 StringRedisTemplate stringRedisTemplate) {
        this.cypherRepository = cypherRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public List<ShopRecommendDTO> getShopsFriendsVisited(Long userId) {
        String cacheKey = RedisConstants.NEO4J_RECOMMEND_FRIENDS_VISITED + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return JSONUtil.toList(cached, ShopRecommendDTO.class);
        }
        try {
            List<ShopRecommendDTO> result = cypherRepository.findShopsFriendsVisited(userId);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), 24, TimeUnit.HOURS);
            return result;
        } catch (Exception e) {
            log.warn("Neo4j查询好友去过的店失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<ShopRecommendDTO> getShopsFriendsLiked(Long userId) {
        String cacheKey = RedisConstants.NEO4J_RECOMMEND_FRIENDS_LIKED + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return JSONUtil.toList(cached, ShopRecommendDTO.class);
        }
        try {
            List<ShopRecommendDTO> result = cypherRepository.findShopsFriendsLiked(userId);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), 24, TimeUnit.HOURS);
            return result;
        } catch (Exception e) {
            log.warn("Neo4j查询好友赞过的店失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<KnownUserDTO> getPeopleYouMightKnow(Long userId, int limit) {
        String cacheKey = RedisConstants.NEO4J_RECOMMEND_MIGHT_KNOW + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return JSONUtil.toList(cached, KnownUserDTO.class);
        }
        try {
            List<KnownUserDTO> result = cypherRepository.findPeopleYouMightKnow(userId, limit);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(result), 24, TimeUnit.HOURS);
            return result;
        } catch (Exception e) {
            log.warn("Neo4j查询可能认识的人失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }
}
