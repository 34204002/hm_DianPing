package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 关注功能前端控制器
 * 提供用户关注相关的REST API接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollow) {
        log.info("用户{}关注用户:{}", UserHolder.getUser().getId(), followUserId);
        followService.follow(followUserId, isFollow);
        return Result.ok();
    }
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        log.info("查询用户{}是否关注用户:{}", UserHolder.getUser().getId(), followUserId);
        return followService.isFollow(followUserId);
    }
    @GetMapping("/common/{id}")
    public Result common(@PathVariable("id") Long id) {
        log.info("查询用户{}和用户{}的共同关注", UserHolder.getUser().getId(), id);
        return followService.common(id);
    }

}
