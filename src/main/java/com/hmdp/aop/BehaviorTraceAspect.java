package com.hmdp.aop;

import com.hmdp.service.IBehaviorAnalysisService;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 行为追踪AOP切面，零侵入记录用户行为数据
 */
@Slf4j
@Aspect
@Component
public class BehaviorTraceAspect {

    @Autowired
    private IBehaviorAnalysisService behaviorAnalysisService;

    /**
     * 用户查看商铺详情：热度+1，记录活跃，记录偏好
     */
    @AfterReturning(value = "execution(* com.hmdp.service.impl.ShopServiceImpl.queryById(..))", returning = "result")
    public void afterShopView(JoinPoint joinPoint, Object result) {
        try {
            Long shopId = (Long) joinPoint.getArgs()[0];
            Long userId = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
            if (userId != null) {
                behaviorAnalysisService.recordActive(userId);
                behaviorAnalysisService.recordHotShop(shopId, 1);
                if (result instanceof Shop shop) {
                    behaviorAnalysisService.recordUserPreference(userId, shop.getTypeId(), "browse");
                }
            }
        } catch (Exception e) {
            log.debug("记录商铺浏览行为失败", e);
        }
    }

    /**
     * 用户点赞博客：活跃，关联商铺热度+3
     */
    @AfterReturning(value = "execution(* com.hmdp.service.impl.BlogServiceImpl.likeBlog(..))")
    public void afterBlogLike(JoinPoint joinPoint) {
        try {
            Long userId = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
            if (userId != null) {
                behaviorAnalysisService.recordActive(userId);
            }
        } catch (Exception e) {
            log.debug("记录博客点赞行为失败", e);
        }
    }

    /**
     * 用户发布博客：关联商铺热度+5，记录偏好
     */
    @AfterReturning(value = "execution(* com.hmdp.service.impl.BlogServiceImpl.saveBlog(..))")
    public void afterBlogSave(JoinPoint joinPoint) {
        try {
            Blog blog = (Blog) joinPoint.getArgs()[0];
            if (blog.getShopId() != null) {
                behaviorAnalysisService.recordHotShop(blog.getShopId(), 5);
                Long userId = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
                if (userId != null) {
                    behaviorAnalysisService.recordActive(userId);
                }
            }
        } catch (Exception e) {
            log.debug("记录博客发布行为失败", e);
        }
    }
}
