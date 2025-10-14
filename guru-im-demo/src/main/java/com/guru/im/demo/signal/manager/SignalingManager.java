package com.guru.im.demo.signal.manager;

import com.guru.im.core.common.listener.SignalMessageSendCallBack;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.signal.handler.CallSignalingHandler;
import com.guru.im.demo.signal.handler.ConferenceSignalingHandler;
import com.guru.im.demo.signal.handler.MediaSignalingHandler;
import com.guru.im.demo.signal.handler.SignalingDispatcher;
import com.guru.im.demo.signal.windows.AbstractMediaWindow;
import com.guru.im.protocol.model.MediasoupConsumerResume;
import com.guru.im.protocol.model.SignalMessageAck;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.protocol.model.SignalingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SignalingManager {
    private static final Logger logger = LoggerFactory.getLogger(SignalingManager.class);

    private final MainFrame mainFrame;
    private final SignalingDispatcher dispatcher;
    private final ResponseWaiterManager responseWaiter;

    public SignalingManager(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.responseWaiter = new ResponseWaiterManager(30000);
        this.dispatcher = new SignalingDispatcher();
        setupHandlers();
    }

    private void setupHandlers() {
        // 注册核心处理器
        dispatcher.registerHandler(new CallSignalingHandler(this));
        dispatcher.registerHandler(new ConferenceSignalingHandler(this));
        dispatcher.registerHandler(new MediaSignalingHandler(this));
    }

    /**
     * 处理来自Netty的信令消息
     */
    public void handleNettySignaling(SignalingMessage message) {
        logger.info("处理信令消息: {}", message.getType());
        // 先检查是否是等待的响应
        if (isResponseMessage(message)) {
            responseWaiter.notifyResponse(message);
        } else {
            // 委托给分发器处理
            dispatcher.dispatch(message);
        }
    }

    public SignalMessageAck sendWithAck(SignalingMessage message, long timeout, TimeUnit unit) {
        try {
            CompletableFuture<SignalMessageAck> future = new CompletableFuture<>();
            // 发送消息
            mainFrame.getImClientManager().signal().sendSignalingMessageWithAck(message, new SignalMessageSendCallBack() {
                @Override
                public void onSendSuccess(SignalMessageAck signalMessageAck) {
                    future.complete(signalMessageAck);
                }

                @Override
                public void onSendFail(String errorMsg) {
                    future.completeExceptionally(new RuntimeException(errorMsg));
                }
            });

            return future.get(timeout, unit);

        } catch (TimeoutException e) {
            logger.error("等待Netty响应超时", e);
            throw new RuntimeException("信令响应超时", e);
        } catch (InterruptedException e) {
            logger.error("等待Netty响应被中断", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("信令响应被中断", e);
        } catch (Exception e) {
            logger.error("转发信令到Netty失败", e);
            throw new RuntimeException("信令发送失败", e);
        }
    }


    /**
     * 转发信令到Netty并等待响应（带超时）
     */
    public SignalingMessage forwardToNetty(SignalingMessage message, long timeout, TimeUnit unit) {
        logger.info("同步发送信令到Netty，并等待响应: type={}, sessionId={}", message.getType(), message.getSessionId());

        SignalMessageAck signalMessageAck = sendWithAck(message, timeout, unit);

        // 等待响应
        SignalingMessage response = null;
        try {
            response = responseWaiter.waitForResponse(
                    signalMessageAck.getMessageId(), timeout, unit);
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        logger.info("收到Netty响应: type={}, sessionId={}",
                response.getType(), response.getSessionId());
        return response;
    }

    /**
     * 异步转发信令到Netty
     */
    public void forwardToNettyAsync(SignalingMessage message) {
        logger.info("异步转发信令到Netty: type={}, sessionId={}",
                message.getType(), message.getSessionId());

        try {
            mainFrame.getImClientManager().signal().sendSignalingMessage(message, null);
        } catch (Exception e) {
            logger.error("转发信令到Netty失败", e);
            throw new RuntimeException("信令发送失败", e);
        }
    }

    /**
     * 转发信令到浏览器
     */
    public void forwardToBrowser(long sessionId, SignalingMessage message) {
        AbstractMediaWindow window = mainFrame.getMediaWindowManager().getActiveMediaWindow(sessionId);
        if (window != null) {
            window.handleSignalingMessage(message);
        } else {
            logger.warn("未找到信令目标窗口: sessionId={}", sessionId);
        }
    }

    // 委托方法
    public MediaWindowManager getMediaWindowManager() {
        return mainFrame.getMediaWindowManager();
    }

    private boolean isResponseMessage(SignalingMessage message) {
        SignalingType type = message.getType();
        switch (type) {
            case CALL_REQUEST_RESPONSE:
            case CONFERENCE_INVITE_RESPONSE:

            case MEDIASOUP_TRANSPORT_CREATE:      // 服务器返回的传输创建响应
            case MEDIASOUP_PRODUCE_RESPONSE:
            case MEDIASOUP_CONSUME_RESPONSE:
            case MEDIASOUP_TRANSPORT_CONNECT_RESPONSE:
            case RTP_CAPABILITIES_RESPONSE:
            case ROOM_PRODUCERS_RESPONSE:
            case MEDIASOUP_CONSUMER_RESUME:
                return true;
            default:
                return false;
        }
    }
}