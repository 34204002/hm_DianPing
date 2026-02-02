package com.hmdp.interceptor;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Token刷新拦截器
 * 功能：如果用户携带了有效的token，则刷新其有效期并存入ThreadLocal
 * 执行时机：最先执行，为后续拦截器提供ThreadLocal中的用户信息
 * 特点：无论token是否有效，都放行请求
 */
@Slf4j
@RequiredArgsConstructor
public class RefreshInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径和token
        String uri = request.getRequestURI();
        String token = request.getHeader("authorization");

        log.info("请求路径: {}, Authorization头部: {}, Token: {}", uri, token != null ? "存在" : "缺失", token != null ? token : "无");

        // 1. 检查请求头中是否包含token
        if (!StringUtils.hasText(token)) {
            // 没有token，直接放行
            log.info("请求 {} 未携带token，放行", uri);
            return true;
        }

        log.info("请求 {} 携带了token，正在验证: {}", uri, token);

        // 2. 从Redis中获取用户信息
        String userJson = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_USER_KEY + token);

        // 3. 判断用户信息是否存在
        if (!StringUtils.hasText(userJson)) {
            // 用户信息不存在，直接放行（不设置ThreadLocal）
            log.info("请求 {} 的token无效或已过期，token: {}，放行但不设置ThreadLocal", uri, token);
            return true;
        }

        log.info("请求 {} 的token验证成功，用户信息存在，token: {}", uri, token);

        // 4. 刷新Token的有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,
                RedisConstants.getLoginUserTtlWithRandomness(), TimeUnit.SECONDS);

        log.info("刷新token成功，请求 {} 放行，token: {}", uri, token);

        // 5. 将用户信息反序列化并存入ThreadLocal
        UserDTO userDTO = JSONUtil.toBean(userJson, UserDTO.class);
        UserHolder.saveUser(userDTO);

        log.info("用户 {} 已存入ThreadLocal，请求 {} 放行，token: {}", userDTO.getId(), uri, token);

        // 6. 放行
        return true;
    }
}
