package com.hmdp;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {
    @Autowired
    private RedissonClient redissonClient;
    @Test
    void tryLock() throws InterruptedException {
        redissonClient.getLock("anyLock").tryLock(5,  TimeUnit.SECONDS);

    }


}
