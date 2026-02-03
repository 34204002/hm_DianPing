package com.hmdp.utils;

import java.util.concurrent.TimeUnit;

public interface ILock {
    /**
     * 尝试获取锁
     * @param key 锁的键
     * @return 是否获取锁成功
     */
    boolean tryLock(String key);

    /**
     * 尝试获取锁
     * @param key 锁的键
     * @param timeout 锁的超时时间
     * @param unit 时间单位
     * @return 是否获取锁成功
     */
    boolean tryLock(String key, long timeout, TimeUnit unit);

    /**
     * 释放锁
     * @param key 锁的键
     */
    void unlock(String key);
}
