package com.guru.im.demo.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.common.constant.MessageType;
import com.guru.im.demo.gui.file.model.FileTransfer;
import com.guru.im.demo.model.MediaInfo;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserConversation;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.util.FileUtil;
import com.guru.im.protocol.model.ConversationType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 本地聊天消息构建
 */
public class MessageBuilder {
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static Message buildTextMessage(String textMessage, UserInfo currentUser, UserConversation userConversation) {
        Message sendMsg = buildBaseMessage(currentUser, userConversation);
        sendMsg.setMessageType(MessageType.TEXT);
        sendMsg.setMessageContent(textMessage);
        return sendMsg;
    }

    public static Message buildFileMessage(FileTransfer transfer, UserInfo currentUser, UserConversation userConversation) {
        Message message = buildBaseMessage(currentUser, userConversation);
        message.setMediaInfo(buildMessageFileBaseInfo(transfer));
        message.setMessageType(getMessageType(transfer));
        try {
            message.setMessageContent(objectMapper.writeValueAsString(message.getMediaInfo()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return message;
    }


    private static Message buildBaseMessage(UserInfo currentUser, UserConversation userConversation) {
        Message message = new Message();
        message.setMessageId(-1L);
        message.setServerSeq(-1L);
        message.setReceiverId(extractReceiverId(currentUser.getUid(), userConversation));
        message.setReceiverName(userConversation.getConversationName());
        message.setSenderId(currentUser.getUid());
        message.setSenderName(currentUser.getUserName());

        message.setClientSendTime(System.currentTimeMillis());
        message.setReadStatus(Message.READ_STATUS_UNREAD);
        message.setClientMsgId(UUID.randomUUID().toString());
        message.setSendStatus(Message.SEND_STATUS_SENDING);
        message.setLocalTemp(true);
        message.setConversationId(userConversation.getConversationId());
        message.setConversationType(userConversation.getConversationType());

        return message;
    }

    private static MediaInfo buildMessageFileBaseInfo(FileTransfer transfer) {
        // 基础信息
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setFileId(transfer.getFileId());
        mediaInfo.setFileSize(transfer.getFileSize());
        mediaInfo.setFileName(transfer.getFileName());
        mediaInfo.setMimeType(transfer.getContentType());
        mediaInfo.setFileType(transfer.getFileType());
        mediaInfo.setLocalFilePath(transfer.getFilePath());
        return mediaInfo;
    }

    public static FileTransfer buildFileDownloadTransfer(Message message) {
        MediaInfo mediaInfo = message.getMediaInfo();
        FileTransfer fileTransfer = new FileTransfer();
        fileTransfer.setId(UUID.randomUUID().toString());
        fileTransfer.setFileId(mediaInfo.getFileId());
        fileTransfer.setFileType(mediaInfo.getFileType());
        fileTransfer.setFileName(mediaInfo.getFileName());
        fileTransfer.setFileSize(mediaInfo.getFileSize());
        fileTransfer.setContentType(mediaInfo.getMimeType());
        fileTransfer.setThumbnailUrl(mediaInfo.getThumbnailUrl());
        fileTransfer.setClientMsgId(message.getClientMsgId());
        fileTransfer.setConversationId(message.getConversationId());
        fileTransfer.setFileUrl(mediaInfo.getFileUrl());
        fileTransfer.setStatus(FileTransfer.Status.PENDING);
        fileTransfer.setCancelled(false);
        fileTransfer.setTransferType(1);
        return fileTransfer;
    }

    private static int getMessageType(FileTransfer transfer) {
        switch (transfer.getFileType()) {
            case FileUtil.FILE_TYPE_IMAGE:
                return MessageType.IMAGE;
            case FileUtil.FILE_TYPE_VIDEO:
                return MessageType.VIDEO;
            default:
                return MessageType.FILE;
        }
    }


    public static Long extractReceiverId(long userId, UserConversation userConversation) {
        try {
            if (userConversation.getConversationType() == ConversationType.PRIVATE_VALUE) {
                List<Long> userIds = Arrays.stream(userConversation.getConversationKey().split("_")).map(Long::parseLong)
                        .toList();
                if (userIds.get(0) == userId) {
                    return userIds.get(1);
                }
                return userIds.get(0);
            } else {
                String groupIdStr = userConversation.getConversationKey().substring(userConversation.getConversationKey().indexOf("_") + 1);
                return Long.parseLong(groupIdStr);
            }

        } catch (Exception e) {
            throw new RuntimeException("get receiverId by conversionKey failed [" + userConversation.getConversationKey() + "]:" + e.getMessage());
        }
    }


}
