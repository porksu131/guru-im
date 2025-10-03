package com.guru.im.cache.starter.distribute.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RedissonDistributedLock implements RedisDistributedLock {
    private static final Logger log = LoggerFactory.getLogger(RedissonDistributedLock.class);
    private final RedissonClient redissonClient;

    public RedissonDistributedLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
    
    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    public <T> T executeWithLock(String lockKey, long waitTime, TimeUnit unit,
                                 Supplier<T> supplier, Supplier<T> fallbackSupplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, unit)) {
                try {
                    return supplier.get();
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
                return fallbackSupplier.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted: {}", lockKey);
            return fallbackSupplier.get();
        }
    }
}