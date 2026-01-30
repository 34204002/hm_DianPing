package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置刷新token拦截器
        registry.addInterceptor(new RefreshInterceptor(stringRedisTemplate));
        // 配置登录拦截器，对需要登录的路径进行拦截
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                    "/user/login",              // 登录接口 - 不需要登录
                    "/user/code",               // 发送验证码接口 - 不需要登录
                    "/shop/**",                 // 商铺相关接口 - 大部分不需要登录
                    "/voucher/**",              // 优惠券相关接口 - 大部分不需要登录
                    "/blog/hot",                // 热门博客 - 不需要登录
                    "/upload/**",             // 上传博客图片 - 可能需要登录，暂不确定
                    "/shop-type/**"          // 商铺类型列表 - 不需要登录
                );


    }
}
