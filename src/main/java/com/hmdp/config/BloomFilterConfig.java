package com.hmdp.config;

import com.hmdp.utils.BloomFilterManager;
import com.hmdp.service.IShopService;
import com.hmdp.service.IUserService;
import com.hmdp.service.IBlogService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Bean
    public ApplicationRunner initializeBloomFilter(BloomFilterManager bloomFilterManager,
                                                  IShopService shopService,
                                                  IUserService userService,
                                                  IBlogService blogService) {
        return args -> {
            // 初始化商铺布隆过滤器
            java.util.List<com.hmdp.entity.Shop> shops = shopService.list();
            for (com.hmdp.entity.Shop shop : shops) {
                bloomFilterManager.getShopBloomFilter().add(shop.getId());
            }

            // 初始化用户布隆过滤器
            java.util.List<com.hmdp.entity.User> users = userService.list();
            for (com.hmdp.entity.User user : users) {
                bloomFilterManager.getUserBloomFilter().add(user.getId());
            }

            // 初始化博客布隆过滤器
            java.util.List<com.hmdp.entity.Blog> blogs = blogService.list();
            for (com.hmdp.entity.Blog blog : blogs) {
                bloomFilterManager.getBlogBloomFilter().add(blog.getId());
            }

            System.out.println("布隆过滤器初始化完成，加载了 " + 
                             shops.size() + " 个商铺, " +
                             users.size() + " 个用户, " +
                             blogs.size() + " 个博客");
        };
    }
}
