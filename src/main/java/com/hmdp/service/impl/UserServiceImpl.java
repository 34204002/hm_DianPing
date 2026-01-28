package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        //1.验证手机号
        //2.如果不符合，返回错误
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5.发送验证码
        log.info("发送验证码成功，验证码：{}", code);

        //6.返回结果
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        // 1. 校验参数
        if (loginForm == null || loginForm.getPhone() == null) {
            return Result.fail("请正确填写登录信息");
        }
        
        // 2. 验证手机号格式
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误");
        }
        
        String phone = loginForm.getPhone();
        
        // 3. 获取用户对象
        User user = query().eq("phone", phone).one();
        
        // 4. 判断用户是否存在
        if (user == null) {
            log.info("用户不存在");
            // 新用户必须通过验证码登录才能创建账户
            if (loginForm.getCode() != null) {
                boolean isValidCode = validateVerificationCode(phone, loginForm.getCode());
                if (isValidCode) {
                    // 验证码正确，创建新用户并登录成功
                    user = User.builder()
                            .phone(phone)
                            .nickName(SystemConstants.USER_NICK_NAME_PREFIX+ RandomUtil.randomString(10))
                            .build();
                    save(user);
                    return Result.ok();
                } else {
                    log.info("登陆失败,验证码错误");
                    return Result.fail("验证码错误");
                }
            } else {
                return Result.fail("用户不存在，请使用验证码注册");
            }
        } else {
            log.info("用户已存在:{}", user);
            // 已存在用户，支持验证码登录或密码登录
            if (loginForm.getCode() != null) {
                // 验证码登录
                boolean isValidCode = validateVerificationCode(phone, loginForm.getCode());
                if (!isValidCode) {
                    log.info("登陆失败,验证码错误");
                    return Result.fail("验证码错误");
                }
            } else if (loginForm.getPassword() != null) {
                // 密码登录
                if (user.getPassword() == null || !user.getPassword().equals(loginForm.getPassword())) {
                    log.info("登陆失败,密码错误");
                    return Result.fail("密码错误");
                }
            } else {
                return Result.fail("请提供登录凭证（验证码或密码）");
            }
            
            return Result.ok();
        }
    }

    /**
     * 验证验证码是否正确，并在验证后删除验证码
     * 主动删除是为了防止验证码被重复使用，提高安全性
     */
    private boolean validateVerificationCode(String phone, String code) {
        String cachedCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if (cachedCode == null || !cachedCode.equals(code)) {
            return false;
        }
        // 验证码使用后立即删除，防止重复使用（防重放攻击）
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + phone);
        return true;
    }
}