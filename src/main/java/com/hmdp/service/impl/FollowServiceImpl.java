package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.repository.Neo4jCypherRepository;
import com.hmdp.service.INeo4jSyncService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IUserService userService;
    @Autowired
    private INeo4jSyncService neo4jSyncService;
    @Autowired
    private Neo4jCypherRepository neo4jCypherRepository;
    @Override
    public void follow(Long followUserId, Boolean isFollow) {
        Follow follow = new Follow();
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(followUserId);
        //如果未关注,则添加关注
        if(BooleanUtil.isFalse(isFollow)){
            boolean isSuccess=save(follow);
            if (isSuccess){
                stringRedisTemplate.opsForSet().add("follow:" + UserHolder.getUser().getId(), String.valueOf(followUserId));
            }
        }
        //如果已关注,则取消关注
        else{
            Boolean isSuccess=remove(new QueryWrapper<Follow>().eq("user_id", follow.getUserId()).eq("follow_user_id", followUserId));
            if(isSuccess){
                stringRedisTemplate.opsForSet().remove("follow:" + UserHolder.getUser().getId(), String.valueOf(followUserId));
            }
        }
        // 同步关注关系到Neo4j，取反是将API的"当前状态"转为"本次操作"
        neo4jSyncService.syncFollow(follow.getUserId(), followUserId, !isFollow);
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        return query().eq("user_id", userId).eq("follow_user_id", followUserId).count() > 0 ? Result.ok(true) : Result.ok(false);

    }

    @Override
    public Result common(Long id) {
        Long currentUserId = UserHolder.getUser().getId();
        List<Long> commonIds = neo4jCypherRepository.findCommonFollows(currentUserId, id);
        if (commonIds.isEmpty())
            return Result.ok(Collections.emptyList());
        List<UserDTO> commonUsers = userService.listByIds(commonIds)
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getNickName(), user.getIcon()))
                .toList();
        return Result.ok(commonUsers);
    }

    /** 从MySQL重建当前用户的Redis关注Set，用于修复不一致 */
    public void rebuildFollowCache(Long userId) {
        List<Follow> follows = query().eq("user_id", userId).list();
        String key = "follow:" + userId;
        stringRedisTemplate.delete(key);
        if (!follows.isEmpty()) {
            String[] ids = follows.stream()
                    .map(f -> String.valueOf(f.getFollowUserId()))
                    .toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(key, ids);
        }
        log.info("Redis关注缓存重建完成: userId={}, count={}", userId, follows.size());
    }
}
