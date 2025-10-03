package com.guru.im.core.server;

import com.guru.im.core.common.exception.SendRequestException;
import com.guru.im.core.common.exception.SendTimeoutException;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.core.manager.ChannelCacheManager;
import com.guru.im.core.manager.MessageProcessManager;
import com.guru.im.protocol.model.ImMessage;
import io.netty.channel.Channel;

public abstract class BaseNettyServer {
    protected ChannelCacheManager channelCacheManager = new ChannelCacheManager();
    protected MessageProcessManager messageProcessManager = new MessageProcessManager();

    public MessageProcessManager getMessageProcessManager() {
        return messageProcessManager;
    }

    public ChannelCacheManager getChannelCacheManager() {
        return channelCacheManager;
    }

    public ImMessage sendSync(final Channel channel, final ImMessage request, final long timeoutMillis)
            throws SendTimeoutException, SendRequestException {
        return this.messageProcessManager.sendSync(channel, request, timeoutMillis);
    }

    public void sendOneway(final Channel channel, final ImMessage request, final long timeoutMillis)
            throws InterruptedException, SendTimeoutException, SendRequestException {
        this.messageProcessManager.invokeOneway(channel, request, timeoutMillis);
    }

    public void sendAsync(final Channel channel, final ImMessage request, final long timeoutMillis,
                          final InvokeCallback invokeCallback) {
        this.messageProcessManager.sendAsync(channel, request, timeoutMillis, invokeCallback);
    }
}
