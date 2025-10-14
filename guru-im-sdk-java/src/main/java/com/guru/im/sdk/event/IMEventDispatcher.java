package com.guru.im.sdk.event;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.util.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IMEventDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(IMEventDispatcher.class);
    private final List<IMEventListener> listeners = new CopyOnWriteArrayList<>();

    private IMEvent convertToEvent(ImMessage imMessage) {
        // 根据消息类型转换为不同事件
        switch (imMessage.getBodyCase()) {
            case CHAT_MESSAGE:
                return new IMEvent(IMEventType.CHAT_MESSAGE_RECEIVED, imMessage, null);
            case SYNC_RESPONSE:
                return new IMEvent(IMEventType.SYNC_MESSAGE_RESPONSE, imMessage, null);
            case SYNC_EVENT_RESPONSE:
                return new IMEvent(IMEventType.SYNC_EVENT_RESPONSE, imMessage, null);
            case FRIEND_REQUEST:
                return new IMEvent(IMEventType.FRIEND_REQUEST_NOTIFY, imMessage, imMessage.getFriendRequest());
            case GROUP_INVITE:
                return new IMEvent(IMEventType.GROUP_INVITE_NOTIFY, imMessage, imMessage.getGroupInvite());
            case SYSTEM_NOTICE:
                return new IMEvent(IMEventType.SYSTEM_NOTICE, imMessage, imMessage.getSystemNotice());
            case PRESENCE_NOTIFY:
                return new IMEvent(IMEventType.USER_PRESENCE, imMessage, imMessage.getPresenceNotify());
            case READ_RECEIPT_NOTIFY:
                return new IMEvent(IMEventType.READ_RECEIPT, imMessage, imMessage.getReadReceiptNotify());
            case OFFLINE_DEVICE_MESSAGE:
                return new IMEvent(IMEventType.OFFLINE_DEVICE, imMessage, imMessage.getOfflineDeviceMessage());
            case SIGNALING_MESSAGE:
                return new IMEvent(IMEventType.SIGNALING_MESSAGE, imMessage, imMessage.getSignalingMessage());
        }
        throw new IllegalArgumentException("Unsupported im message type: " + imMessage.getBodyCase());
    }

    // 单向消息，无需响应ack
    public void dispatchOneway(ImMessage imMessage) {
        IMEvent event = convertToEvent(imMessage);
        dispatchOneway(event);
    }

    public void dispatchOneway(IMEvent imEvent) {
        for (IMEventListener listener : listeners) {
            if (listener.canHandle(imEvent)) {
                listener.onEvent(imEvent);
            }
        }
    }

    // 双向消息，需要响应ack
    public ImMessage dispatch(ImMessage imMessage) {
        IMEvent event = convertToEvent(imMessage);
        return dispatch(event);
    }

    public ImMessage dispatch(IMEvent imEvent) {
        for (IMEventListener listener : listeners) {
            if (!listener.canHandle(imEvent)) {
                continue;
            }
            listener.onEvent(imEvent);
            if (listener.isCustomerResponse()) {
                // 如果有自定义的ack响应，则由监听器负责构建ack响应
                return listener.Response(imEvent);
            }
        }
        // 默认响应成功ack
        return MessageBuilder.createImResponse(imEvent.getImMessage(), ResponseCode.SUCCESS, "message received");
    }

    public void addEventListener(IMEventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(IMEventListener listener) {
        listeners.remove(listener);
    }
}