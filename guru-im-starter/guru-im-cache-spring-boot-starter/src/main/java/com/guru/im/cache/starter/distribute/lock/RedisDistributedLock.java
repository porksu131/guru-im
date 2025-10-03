package com.guru.im.cache.starter.distribute.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface RedisDistributedLock {
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    void unlock(String lockKey);

    boolean isLocked(String lockKey);

    <T> T executeWithLock(String lockKey, long waitTime, TimeUnit unit,
                          Supplier<T> supplier, Supplier<T> fallbackSupplier);
}
