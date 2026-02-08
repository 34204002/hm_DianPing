package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson基础配置类
 */
@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 单节点配置 - 使用application.yml中的Redis配置
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setDatabase(1)
                .setPassword(null)  // 如果有密码可以设置
                .setTimeout(5000)
                .setConnectTimeout(5000);
        
        return Redisson.create(config);
    }
}