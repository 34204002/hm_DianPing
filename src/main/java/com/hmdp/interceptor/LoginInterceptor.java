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
 * 登录验证拦截器
 * 功能：验证用户是否已登录（检查ThreadLocal中是否有用户信息）
 * 执行时机：在刷新拦截器之后执行，此时ThreadLocal中已有用户信息（如果token有效）
 * 使用场景：需要强制登录的接口
 */
@Slf4j
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("开始处理请求 {}", request.getRequestURI());
        
        String uri = request.getRequestURI();
        
        // 检查ThreadLocal中是否有用户信息（由刷新拦截器设置）
        UserDTO user = UserHolder.getUser();
        
        if (user == null) {
            // ThreadLocal中没有用户信息，说明用户未登录或token无效，拦截请求
            log.warn("请求 {} 用户未登录，已被拦截", uri);
            response.setStatus(401);
            return false;
        }
        
        log.info("请求 {} 用户已登录，用户ID: {}, 放行", uri, user.getId());
        
        // 用户已登录，放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String uri = request.getRequestURI();
        log.info("请求 {} 处理完成，清理ThreadLocal", uri);
        
        // 清理ThreadLocal中的用户信息，防止内存泄漏
        UserHolder.removeUser();
    }
}
