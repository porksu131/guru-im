package com.guru.im.core.im;

import com.guru.im.core.client.DefaultNettyClient;
import com.guru.im.core.common.exception.NettyConnectException;
import com.guru.im.core.common.exception.SendRequestException;
import com.guru.im.core.common.exception.SendTimeoutException;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.core.common.thread.ThreadFactoryImpl;
import com.guru.im.core.im.config.ImClientConfig;
import com.guru.im.core.im.exception.ConnectException;
import com.guru.im.core.im.handler.AuthHandler;
import com.guru.im.core.im.handler.HeartbeatHandler;
import com.guru.im.core.im.handler.IMClientHandler;
import com.guru.im.core.im.listener.ConnectionListener;
import com.guru.im.core.im.listener.MessageListener;
import com.guru.im.core.im.manager.ReconnectManager;
import com.guru.im.core.im.processor.IMMessageProcessor;
import com.guru.im.core.im.state.ConnectionState;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import io.netty.channel.ChannelPipeline;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IMClient extends DefaultNettyClient {
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile boolean forceOffline = false;
    private final ReconnectManager reconnectManager;
    private final ImClientConfig config;
    private final ConnectionListener connectionListener;
    private final MessageListener messageListener;
    private final ExecutorService messageExecutor;

    public IMClient(ImClientConfig config) {
        super(config);
        config.setClientChannelMaxIdleTimeSeconds(config.getHeartbeatInterval());
        this.config = config;
        this.connectionListener = config.getConnectionListener();
        this.messageListener = config.getMessageListener();
        this.reconnectManager = new ReconnectManager(this);
        this.messageExecutor = Executors.newFixedThreadPool(4, new ThreadFactoryImpl("MessageExecutor_"));
        IMMessageProcessor imMessageProcessor = new IMMessageProcessor(this.messageListener);
        this.getMessageProcessManager().bindRequestProcessor(imMessageProcessor, this.messageExecutor);
    }

    public void configChannelHandler(ChannelPipeline pipeline) {
        pipeline.addLast(defaultEventExecutorGroup, new AuthHandler(this.config.getToken(), this.config.getDeviceInfo()));
        pipeline.addLast(defaultEventExecutorGroup, new HeartbeatHandler(this));
        pipeline.addLast(defaultEventExecutorGroup, new IMClientHandler(this));
    }

    public void connect() {
        try {
            if (forceOffline) {
                return;
            }
            connectionListener.onReconnecting(reconnectManager.attemptCount());
            super.connect(config.getConnectAddress());
            state = ConnectionState.CONNECTED;
            connectionListener.onConnected();
            reconnectManager.reset();
        } catch (Exception e) {
            state = ConnectionState.DISCONNECTED;
            connectionListener.onConnectFailed(e.getMessage());
            if (config.isAutoReconnect() && !forceOffline) {
                reconnectManager.setReconnecting(false);
                reconnectManager.scheduleReconnect();
            }
        }
    }

    public void reconnect() {
        if (forceOffline) {
            return;
        }
        reconnectManager.scheduleReconnect();
    }

    public void sendOneway(ImMessage request, long timeoutMillis)
            throws SendRequestException, SendTimeoutException, InterruptedException {
        checkConnection();
        super.sendOneway(this.config.getConnectAddress(), request, timeoutMillis);
    }

    public ImMessage sendMessageSync(ImMessage request, long timeoutMillis)
            throws SendRequestException, SendTimeoutException, NettyConnectException {
        checkConnection();
        return super.sendMessageSync(this.config.getConnectAddress(), request, timeoutMillis);
    }

    public void sendMessageAsync(ImMessage request, long timeoutMillis, InvokeCallback invokeCallback)
            throws SendTimeoutException, NettyConnectException {
        checkConnection();
        super.sendMessageAsync(this.config.getConnectAddress(), request, timeoutMillis, invokeCallback);
    }

    public void disconnect() {
        if (state != ConnectionState.DISCONNECTED) {
            forceOffline = true;
            super.disConnect(config.getConnectAddress());
            state = ConnectionState.DISCONNECTED;
            connectionListener.onDisconnected();
            config.setToken(null);
        }
    }

    public void shutdown() {
        super.shutdown();
        reconnectManager.shutdown();
        messageExecutor.shutdown();
    }


    public void checkConnection() {
        if (state != ConnectionState.CONNECTED) {
            throw new ConnectException("Client is not connected");
        }
    }

    public ImMessage sendAuthMessage() throws Exception {
        ImMessage authMessage = MessageBuilder.createAuthMessage(this.config.getToken());
        return this.sendMessageSync(config.getConnectAddress(), authMessage, 10000);
    }

    public void sendHeartbeat() throws Exception {
        ImMessage heartbeat = MessageBuilder.createHeartbeat();
        this.sendOneway(config.getConnectAddress(), heartbeat, 5000);
    }


    public ConnectionState getState() {
        return state;
    }

    public ReconnectManager getReconnectManager() {
        return reconnectManager;
    }

    public ImClientConfig getConfig() {
        return config;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }

    public boolean isForceOffline() {
        return forceOffline;
    }
}