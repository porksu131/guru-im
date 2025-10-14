package com.guru.im.protocol.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SequenceIdUtil {

    private static final SequenceIdUtil INSTANCE = new SequenceIdUtil();

    public static SequenceIdUtil getInstance() {
        return INSTANCE;
    }

    // 动态基准时间，可以自动调整
    private final AtomicLong dynamicBaseTime = new AtomicLong(1704067200000L); // 2024-01-01 00:00:00

    // 位数分配
    private static final long TIME_BITS = 31;      // 时间差值位
    private static final long COUNTER_BITS = 2;    // 计数器位
    private static final long RANDOM_BITS = 6;     // 增加随机位比例

    private static final int MAX_COUNTER = (1 << COUNTER_BITS) - 1;
    private static final long MAX_TIME_DIFF = (1L << TIME_BITS) - 1;

    private volatile long lastTimestamp = -1L;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final int instanceRandom = ThreadLocalRandom.current().nextInt(1 << RANDOM_BITS);

    // 统计信息
    private final AtomicLong totalGenerated = new AtomicLong(0);
    private final AtomicLong fallbackCount = new AtomicLong(0);

    public int next() {
        long currentTimestamp = System.currentTimeMillis();
        int currentCounter;

        while (true) {
            long lastTs = this.lastTimestamp;

            if (currentTimestamp > lastTs) {
                if (counter.compareAndSet(counter.get(), 0)) {
                    this.lastTimestamp = currentTimestamp;
                    currentCounter = 0;
                    break;
                }
            } else if (currentTimestamp == lastTs) {
                currentCounter = counter.incrementAndGet();
                if (currentCounter <= MAX_COUNTER) {
                    break;
                } else {
                    currentTimestamp = waitNextMillis(currentTimestamp);
                    continue;
                }
            } else {
                handleClockBackwards(lastTs, currentTimestamp);
                currentTimestamp = System.currentTimeMillis();
                continue;
            }
        }

        long baseTime = dynamicBaseTime.get();
        long timeDiff = currentTimestamp - baseTime;

        int id;
        if (timeDiff <= 0) {
            // 时间早于基准时间，使用纯随机ID
            id = generateFallbackId(currentTimestamp);
        } else if (timeDiff > MAX_TIME_DIFF) {
            // 时间耗尽，自动调整基准时间或使用降级方案
            id = handleTimeExhausted(currentTimestamp, baseTime);
        } else {
            // 正常生成ID
            id = generateNormalId(timeDiff, currentCounter);
        }

        totalGenerated.incrementAndGet();
        return id;
    }

    /**
     * 正常生成ID
     */
    private int generateNormalId(long timeDiff, int currentCounter) {
        long id = (timeDiff << (COUNTER_BITS + RANDOM_BITS))
                | ((long) currentCounter << RANDOM_BITS)
                | (ThreadLocalRandom.current().nextInt(1 << RANDOM_BITS));

        return ensureIntRange(id);
    }

    /**
     * 处理时间耗尽的情况
     */
    private int handleTimeExhausted(long currentTimestamp, long oldBaseTime) {
        fallbackCount.incrementAndGet();

        // 策略1：自动调整基准时间
        long newBaseTime = currentTimestamp - (MAX_TIME_DIFF / 2);
        if (dynamicBaseTime.compareAndSet(oldBaseTime, newBaseTime)) {
            // 基准时间调整成功，重新计算
            long timeDiff = currentTimestamp - newBaseTime;
            if (timeDiff > 0 && timeDiff <= MAX_TIME_DIFF) {
                return generateNormalId(timeDiff, counter.get());
            }
        }

        // 策略2：使用时间取模（循环使用时间范围）
        return generateModuloId(currentTimestamp);
    }

    /**
     * 使用时间取模的方式生成ID（循环使用时间范围）
     */
    private int generateModuloId(long currentTimestamp) {
        long baseTime = dynamicBaseTime.get();
        long timeDiff = (currentTimestamp - baseTime) % (MAX_TIME_DIFF / 2);
        if (timeDiff < 0) timeDiff = -timeDiff;

        int randomCounter = ThreadLocalRandom.current().nextInt(1 << COUNTER_BITS);
        int randomValue = ThreadLocalRandom.current().nextInt(1 << RANDOM_BITS);

        long id = (timeDiff << (COUNTER_BITS + RANDOM_BITS))
                | ((long) randomCounter << RANDOM_BITS)
                | randomValue;

        return ensureIntRange(id);
    }

    /**
     * 降级方案：生成纯随机ID
     */
    private int generateFallbackId(long currentTimestamp) {
        // 使用时间戳的低位 + 高随机性
        int timeLow = (int) (currentTimestamp & 0x7FFF); // 取15位时间低位
        int randomHigh = ThreadLocalRandom.current().nextInt(1 << 17); // 17位随机数

        int id = (timeLow << 17) | randomHigh;

        // 设置最高位为1，标识这是降级ID
        id |= 0x80000000;

        return id;
    }

    /**
     * 确保ID在int范围内
     */
    private int ensureIntRange(long id) {
        if (id > Integer.MAX_VALUE) {
            // 如果超出范围，取模回绕，但保留位结构
            return (int) (id & 0x7FFFFFFF);
        }
        return (int) id;
    }

    /**
     * 处理时钟回拨
     */
    private void handleClockBackwards(long lastTimestamp, long currentTimestamp) {
        long offset = lastTimestamp - currentTimestamp;

        if (offset <= 1000) {
            // 小范围回拨，短暂等待
            try {
                Thread.sleep(offset + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else if (offset <= 60000) {
            // 中等范围回拨，调整基准时间
            dynamicBaseTime.set(currentTimestamp - (MAX_TIME_DIFF / 4));
            this.lastTimestamp = currentTimestamp;
            counter.set(0);
        } else {
            // 大范围回拨，使用时间取模策略
            this.lastTimestamp = currentTimestamp;
            counter.set(ThreadLocalRandom.current().nextInt(MAX_COUNTER + 1));
        }
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        int waitCount = 0;
        while (timestamp <= lastTimestamp && waitCount < 100) {
            Thread.yield();
            timestamp = System.currentTimeMillis();
            waitCount++;
        }
        return timestamp;
    }

    /**
     * 获取生成器状态
     */
    public String getStatus() {
        long currentTime = System.currentTimeMillis();
        long baseTime = dynamicBaseTime.get();
        long timeDiff = currentTime - baseTime;
        double usagePercent = Math.min(100.0, (double) timeDiff / MAX_TIME_DIFF * 100);

        return String.format(
                "SequenceID状态: 基准时间=%tF %<tT, 当前时间=%tF %<tT, 时间差=%d, 最大允许=%d, 使用率=%.4f%%, " +
                        "总生成数=%d, 降级次数=%d, 降级率=%.4f%%",
                baseTime, baseTime, currentTime, currentTime, timeDiff, MAX_TIME_DIFF, usagePercent,
                totalGenerated.get(), fallbackCount.get(),
                totalGenerated.get() > 0 ? (double) fallbackCount.get() / totalGenerated.get() * 100 : 0
        );
    }

    /**
     * 手动调整基准时间（用于特殊情况）
     */
    public void adjustBaseTime(long newBaseTime) {
        long currentTime = System.currentTimeMillis();
        if (newBaseTime < currentTime - MAX_TIME_DIFF) {
            newBaseTime = currentTime - (MAX_TIME_DIFF / 2);
        }
        dynamicBaseTime.set(newBaseTime);
        this.lastTimestamp = -1L;
        counter.set(0);
    }

    /**
     * 判断ID是否为降级生成的ID
     */
    public boolean isFallbackId(int id) {
        return (id & 0x80000000) != 0;
    }

    /**
     * 解析ID信息
     */
    public void parseId(int id) {
        if (isFallbackId(id)) {
            System.out.printf("降级ID: %d (最高位为1标识降级)%n", id);
        } else {
            long timePart = (id >>> (COUNTER_BITS + RANDOM_BITS)) & ((1L << TIME_BITS) - 1);
            int counterPart = (id >>> RANDOM_BITS) & MAX_COUNTER;
            int randomPart = id & ((1 << RANDOM_BITS) - 1);

            long actualTime = dynamicBaseTime.get() + timePart;

            System.out.printf("正常ID: %d -> 生成时间: %tF %tT, 计数器: %d, 随机数: %d%n",
                    id, actualTime, actualTime, counterPart, randomPart);
        }
    }
}