package com.guru.im.core.im.manager;

import com.guru.im.core.im.IMClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReconnectManager {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final IMClient client;
    private volatile boolean isReconnecting = false;
    private int attemptCount = 0;
    private int maxAttempts = 10;
    private long initialDelay = 1000;
    private long maxDelay = 60000;

    public ReconnectManager(IMClient client) {
        this.client = client;
        this.maxAttempts = client.getConfig().getMaxReconnectAttempts();
        this.initialDelay = client.getConfig().getInitialReconnectDelay();
        this.maxDelay = client.getConfig().getMaxReconnectDelay();
    }

    public void scheduleReconnect() {
        if (isReconnecting || attemptCount >= maxAttempts) return;

        isReconnecting = true;
        attemptCount++;

        // 指数退避算法
        long delay = Math.min(initialDelay * (long) Math.pow(2, attemptCount - 1), maxDelay);

        scheduler.schedule(() -> {
            try {
                client.connect(); // 触发重连
            } finally {
                // 这里只需要确保异常情况下不会阻塞后续重连即可
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void reset() {
        attemptCount = 0;
        isReconnecting = false;
    }

    public void setReconnecting(boolean isReconnecting) {
        this.isReconnecting = isReconnecting;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public int attemptCount() {
        return attemptCount;
    }
}