package com.guru.im.demo.listener;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.*;
import com.guru.im.protocol.util.MessageBuilder;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

import java.util.List;
import java.util.stream.Collectors;

public class SyncMessageListener implements IMEventListener {

    private final MainFrame mainFrame;

    public SyncMessageListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.SYNC_MESSAGE_RESPONSE == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        ImMessage imMessage = event.getImMessage();
        OfflineSyncResponse syncResponse = imMessage.getSyncResponse();
        if (syncResponse.getSyncStatus() != SyncStatus.SYNC_STATUS_FAILED) {
            List<ChatMessage> chatMessages = getChatMessages(syncResponse);
            mainFrame.onReceiveChatMessage(chatMessages);
        }
    }

    @Override
    public boolean isCustomerResponse() {
        return true;
    }

    @Override
    public ImMessage Response(IMEvent event) {
        ImMessage imMessage = event.getImMessage();
        OfflineSyncResponse syncResponse = imMessage.getSyncResponse();
        List<ChatMessage> chatMessages = getChatMessages(syncResponse);
        OfflineSyncAck syncAck = OfflineSyncAck.newBuilder()
                .setSyncId(syncResponse.getSyncId())
                .setBatchNumber(syncResponse.getCurrentBatch())
                .setSuccess(true)
                .setReceivedCount(syncResponse.getMessagesList().size())
                .setUserId(syncResponse.getUserId())
                .addAllReceivedMsgIds(chatMessages.stream().map(ChatMessage::getMessageId).collect(Collectors.toList()))
                .setDeviceId(syncResponse.getDeviceId())
                .setFinal(syncResponse.getFinal())
                .setSyncType(syncResponse.getSyncType())
                .putAllConfirmedCursorMap(syncResponse.getServerCursorMapMap())
                .build();

        return MessageBuilder.createDefaultImMessage().toBuilder()
                .setMsgId(imMessage.getMsgId())
                .setMsgType(ImMessage.MsgType.RESPONSE)
                .setMessageType(MessageType.OFFLINE_MSG_ACK)
                .setSyncAck(syncAck)
                .build();
    }

    private List<ChatMessage> getChatMessages(OfflineSyncResponse syncResponse) {
        List<ImMessage> imMessageList = syncResponse.getMessagesList().stream().map(obj -> {
            try {
                return ImMessage.parseFrom(obj.toByteArray());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        return imMessageList.stream()
                .filter(obj -> obj.getBodyCase() == ImMessage.BodyCase.CHAT_MESSAGE)
                .map(ImMessage::getChatMessage)
                .toList();
    }
}
