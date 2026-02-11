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
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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
    @Override
    public void follow(Long followUserId, Boolean isFollow) {
        Follow follow = new Follow();
        follow.setUserId(UserHolder.getUser().getId());
        follow.setFollowUserId(followUserId);
        //如果未关注,则添加关注
        if(BooleanUtil.isTrue(isFollow)){
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
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        return query().eq("user_id", userId).eq("follow_user_id", followUserId).count() > 0 ? Result.ok(true) : Result.ok(false);

    }

    @Override
    public Result common(Long id) {
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect("follow:" + id, "follow:" + UserHolder.getUser().getId());
        if (intersect==null||intersect.isEmpty())
            return Result.ok();
        List<Long> list=intersect.stream().map(Long::valueOf).toList();
        List<UserDTO> list1 = userService.listByIds(list)
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getNickName(), user.getIcon()))
                .toList();
        return Result.ok(list1);
    }
}
