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
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     *
     * @param phone the phone
     * @return the result
     */
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

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @return the result
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        log.info("登录：{}", loginForm);
        try {
            String token = userService.login(loginForm);
            log.info("登录成功");
            return Result.ok(token);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * @param request HTTP请求对象
     * @return 操作结果 result
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        log.info("登出");
        // 从请求头获取token
        String token = request.getHeader("authorization");
        if (token != null && !token.isEmpty()) {
            // 调用服务层登出方法，清除Redis中的用户信息
            userService.logout(token);
        }
        return Result.ok("登出成功");
    }

    /**
     * 用户个人详情
     *
     * @return the result
     */
    @GetMapping("/me")
    public Result me(){
        log.info("获取当前登录用户:{}", UserHolder.getUser());
        return Result.ok(userService.queryById(UserHolder.getUser().getId()));

    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户详细信息 result
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.queryUserInfoById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long userId){
        User user = userService.queryById(userId);
        if(user == null)
            return Result.fail("用户不存在");
        UserDTO userDTO = new UserDTO(user.getId(), user.getNickName(), user.getIcon());
        return Result.ok(userDTO);
    }
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}