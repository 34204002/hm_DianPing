package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户关注接口，支持关注/取关和共同关注查询
 */
@Slf4j
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    /** 关注/取关用户 */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        log.info("用户{}关注用户:{}", UserHolder.getUser().getId(), followUserId);
        followService.follow(followUserId, isFollow);
        return Result.ok();
    }

    /** 判断是否已关注 */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        log.info("查询用户{}是否关注用户:{}", UserHolder.getUser().getId(), followUserId);
        return followService.isFollow(followUserId);
    }

    /** 查询共同关注 */
    @GetMapping("/common/{id}")
    public Result common(@PathVariable("id") Long id) {
        log.info("查询用户{}和用户{}的共同关注", UserHolder.getUser().getId(), id);
        return followService.common(id);
    }

    /** 从MySQL重建用户的Redis关注缓存，用于修复数据不一致 */
    @PostMapping("/rebuild-cache/{id}")
    public Result rebuildCache(@PathVariable("id") Long id) {
        followService.rebuildFollowCache(id);
        return Result.ok("关注缓存重建完成");
    }
}
