package com.hmdp.controller;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户接口，支持登录注册、签到统计和个人信息查询
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /** 发送手机验证码 */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        log.info("发送验证码：{}", phone);
        try {
            userService.sendCode(phone);
            log.info("发送验证码成功");
            return Result.ok();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 用户登录 */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        log.info("登录：{}", loginForm);
        try {
            String token = userService.login(loginForm);
            log.info("登录成功");
            return Result.ok(token);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 用户登出 */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        log.info("登出");
        String token = request.getHeader("authorization");
        if (token != null && !token.isEmpty()) {
            userService.logout(token);
        }
        return Result.ok("登出成功");
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result me() {
        log.info("获取当前登录用户:{}", UserHolder.getUser());
        return Result.ok(userService.queryById(UserHolder.getUser().getId()));
    }

    /** 查询用户扩展信息 */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.queryUserInfoById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }

    /** 根据ID查询用户基本信息 */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long userId) {
        User user = userService.queryById(userId);
        if (user == null)
            return Result.fail("用户不存在");
        UserDTO userDTO = new UserDTO(user.getId(), user.getNickName(), user.getIcon());
        return Result.ok(userDTO);
    }

    /** 每日签到 */
    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }

    /** 查询签到统计 */
    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }
}
