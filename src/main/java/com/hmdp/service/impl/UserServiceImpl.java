package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.BloomFilterManager;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.ILock;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private CacheClient cacheClient;
    @Autowired
    private BloomFilterManager bloomFilterManager;
    @Autowired
    private IUserInfoService userInfoService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据ID查询用户，使用布隆过滤器优化
     */
    public User queryById(Long id) {
        RBloomFilter<Long> bloomFilter = bloomFilterManager.getUserBloomFilter();

        return cacheClient.queryWithBloomFilterAndProtection(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                id,
                User.class,
                super::getById,
                bloomFilter
        );
    }

    @Override
    public void sendCode(String phone) {
        //1.验证手机号
        //2.如果不符合，抛出异常
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new RuntimeException("手机号格式错误");
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5.发送验证码
        log.info("发送验证码成功，验证码：{}", code);
    }

    @Override
    public String login(LoginFormDTO loginForm) {
        // 1. 校验参数
        if (loginForm == null || loginForm.getPhone() == null) {
            throw new RuntimeException("请正确填写登录信息");
        }

        // 2. 验证手机号格式
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            throw new RuntimeException("手机号格式错误");
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
                            .nickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10))
                            .build();
                    save(user);

                    // 创建用户信息
                    createUserInfoIfNotExists(user.getId());

                    // 生成Session Token并保存用户信息到Redis
                    String token = generateToken();
                    log.info("生成新用户并创建token: {}, 用户ID: {}", token, user.getId());

                    stringRedisTemplate.opsForValue().set(
                            RedisConstants.LOGIN_USER_KEY + token,
                            JSONUtil.toJsonStr(user),
                            RedisConstants.getLoginUserTtlWithRandomness(),
                            TimeUnit.MINUTES
                    );

                    return token;
                } else {
                    log.info("登陆失败,验证码错误");
                    throw new RuntimeException("验证码错误");
                }
            } else {
                throw new RuntimeException("用户不存在，请使用验证码注册");
            }
        } else {
            log.info("用户已存在:{}", user);
            // 已存在用户，支持验证码登录或密码登录
            if (loginForm.getCode() != null) {
                // 验证码登录
                boolean isValidCode = validateVerificationCode(phone, loginForm.getCode());
                if (!isValidCode) {
                    log.info("登陆失败,验证码错误");
                    throw new RuntimeException("验证码错误");
                }
            } else if (loginForm.getPassword() != null) {
                // 密码登录
                if (user.getPassword() == null || !user.getPassword().equals(loginForm.getPassword())) {
                    log.info("登陆失败,密码错误");
                    throw new RuntimeException("密码错误");
                }
            } else {
                throw new RuntimeException("请提供登录凭证（验证码或密码）");
            }

            // 登录成功，检查并创建用户信息（如果不存在）
            createUserInfoIfNotExists(user.getId());

            // 生成Session Token并保存用户信息到Redis
            String token = generateToken();
            log.info("用户登录成功并创建token: {}, 用户ID: {}", token, user.getId());

            stringRedisTemplate.opsForValue().set(
                    RedisConstants.LOGIN_USER_KEY + token,
                    JSONUtil.toJsonStr(user),
                    RedisConstants.getLoginUserTtlWithRandomness(),
                    TimeUnit.MINUTES
            );

            return token;
        }
    }

    @Override
    public void logout(String token) {
        stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
    }

    /**
     * 生成安全的Session Token
     * @return 随机生成的token
     */
    private String generateToken() {
        // 使用UUID生成唯一的Session Token，更安全
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        log.info("生成Session Token: {}", token);
        return token;
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

    /**
     * 如果用户信息不存在，则创建一个新的用户信息
     */
    private void createUserInfoIfNotExists(Long userId) {
        // 使用MyBatis-Plus的query方法检查用户信息是否存在
        UserInfo existingUserInfo = userInfoService.query()
            .eq("user_id", userId)
            .one();

        if (existingUserInfo == null) {
            // 用户信息不存在，创建一个新的
            UserInfo newUserInfo = new UserInfo();
            newUserInfo.setUserId(userId); // 设置关联的用户ID
            // 设置合理的默认值
            newUserInfo.setCity(""); // 默认城市为空
            newUserInfo.setIntroduce(""); // 默认介绍为空
            newUserInfo.setFans(0); // 默认粉丝数为0
            newUserInfo.setFollowee(0); // 默认关注数为0
            newUserInfo.setGender(false); // 默认性别为男(false表示男，true表示女)
            newUserInfo.setBirthday(null); // 默认生日为空
            newUserInfo.setCredits(0); // 默认积分为0
            newUserInfo.setLevel(false); // 默认会员等级为非会员
            newUserInfo.setCreateTime(LocalDateTime.now()); // 设置创建时间
            newUserInfo.setUpdateTime(LocalDateTime.now()); // 设置更新时间

            // 保存到数据库
            userInfoService.save(newUserInfo);

            log.info("为用户 {} 创建了默认的用户信息", userId);
        } else {
            log.info("用户 {} 的信息已存在，无需重复创建", userId);
        }
    }
}