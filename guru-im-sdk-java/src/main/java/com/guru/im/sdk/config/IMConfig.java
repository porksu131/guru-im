package com.guru.im.sdk.config;

import com.guru.im.core.im.config.ImClientConfig;

public class IMConfig {
    private final String serverAddress;
    private final String appKey;
    private final boolean autoReconnect;
    private final long sendTimeout;
    private final int heartbeatInterval;
    private final int maxReconnectAttempts;
    private final long maxReconnectDelay;
    private final long initialReconnectDelay;

    public static ImClientConfig convert(IMConfig config) {
        ImClientConfig clientConfig = new ImClientConfig();
        clientConfig.setConnectAddress(config.getServerAddress());
        clientConfig.setAutoReconnect(true);
        clientConfig.setHeartbeatInterval(config.getHeartbeatInterval());
        clientConfig.setSendTimeout(config.getSendTimeout());
        clientConfig.setInitialReconnectDelay(config.getInitialReconnectDelay());
        clientConfig.setMaxReconnectAttempts(config.getMaxReconnectAttempts());
        clientConfig.setMaxReconnectDelay(config.getMaxReconnectDelay());
        return clientConfig;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getAppKey() {
        return appKey;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public long getSendTimeout() {
        return sendTimeout;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public long getMaxReconnectDelay() {
        return maxReconnectDelay;
    }

    public long getInitialReconnectDelay() {
        return initialReconnectDelay;
    }


    private IMConfig(Builder builder) {
        serverAddress = builder.serverAddress;
        appKey = builder.appKey;
        autoReconnect = builder.autoReconnect;
        sendTimeout = builder.sendTimeout;
        heartbeatInterval = builder.heartbeatInterval;
        maxReconnectAttempts = builder.maxReconnectAttempts;
        maxReconnectDelay = builder.maxReconnectDelay;
        initialReconnectDelay = builder.initialReconnectDelay;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String serverAddress;
        private String appKey;
        private boolean autoReconnect;
        private long sendTimeout;
        private int maxReconnectAttempts;
        private long maxReconnectDelay;
        private long initialReconnectDelay;
        private int heartbeatInterval;

        private Builder() {
        }

        public Builder serverAddress(String val) {
            serverAddress = val;
            return this;
        }

        public Builder appKey(String val) {
            appKey = val;
            return this;
        }

        public Builder autoReconnect(boolean val) {
            autoReconnect = val;
            return this;
        }

        public Builder sendTimeout(long val) {
            sendTimeout = val;
            return this;
        }

        public Builder maxReconnectAttempts(int val) {
            maxReconnectAttempts = val;
            return this;
        }

        public Builder maxReconnectDelay(long val) {
            maxReconnectDelay = val;
            return this;
        }

        public Builder initialReconnectDelay(long val) {
            initialReconnectDelay = val;
            return this;
        }

        public IMConfig build() {
            return new IMConfig(this);
        }

        public Builder heartbeatInterval(int val) {
            heartbeatInterval = val;
            return this;
        }
    }
}