package com.guru.im.demo.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.demo.model.*;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.util.FileUtil;
import com.guru.im.protocol.model.*;

import java.util.UUID;

/**
 * IM协议消息与本地消息模型的互相转换
 */
public class MessageConverter {
    public static FriendRequest convertToClientFriendRequest(FriendRequestNotify friendRequestNotify) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(friendRequestNotify.getId());
        friendRequest.setRequesterId(friendRequestNotify.getRequesterId());
        friendRequest.setRequesterName(friendRequestNotify.getRequesterName());
        friendRequest.setRequestMsg(friendRequestNotify.getRequestMsg());
        friendRequest.setRequestType(friendRequestNotify.getRequestTypeValue());
        friendRequest.setRequestStatus(friendRequestNotify.getRequestStatusValue());
        friendRequest.setCreateTime(friendRequestNotify.getCreateTime());
        friendRequest.setResponderId(friendRequestNotify.getResponderId());
        friendRequest.setResponderName(friendRequestNotify.getResponderName());
        friendRequest.setResponseTime(friendRequestNotify.getResponseTime());
        return friendRequest;
    }

    public static GroupInvite convertToClientGroupInvite(GroupInviteNotify groupInviteNotify) {
        GroupInvite groupInvite = new GroupInvite();
        groupInvite.setId(groupInviteNotify.getId());
        groupInvite.setGroupId(groupInviteNotify.getGroupId());
        groupInvite.setGroupName(groupInviteNotify.getGroupName());
        groupInvite.setGroupAvatar(groupInviteNotify.getGroupAvatar());
        groupInvite.setInviterId(groupInviteNotify.getInviterId());
        groupInvite.setInviterName(groupInviteNotify.getInviterName());
        groupInvite.setGlobalSeq(groupInviteNotify.getGlobalSeq());
        groupInvite.setRequestStatus(groupInviteNotify.getRequestStatus());
        groupInvite.setInviteReason(groupInviteNotify.getInviteReason());
        groupInvite.setInitialMembers(groupInviteNotify.getInitialMembersList());
        return groupInvite;
    }

    public static Notification convertToClientGroupInviteNotify(GroupInviteNotify groupInviteNotify) {
        Notification notification = new Notification();
        notification.setId(groupInviteNotify.getId());
        notification.setCorrelationId(groupInviteNotify.getId());
        notification.setRead(false);
        notification.setTimestamp(groupInviteNotify.getCreateTime());
        notification.setType(Notification.Type.GROUP_INVITE.getCode());
        notification.setTitle(Notification.Type.GROUP_INVITE.getDes());
        notification.setContent("您已被邀请加入群聊“" + groupInviteNotify.getGroupName() + "”。");
        return notification;
    }

    public static Notification convertToClientFriendNotify(UserInfo currentUser, FriendRequestNotify friendRequestNotify) {
        Notification notification = new Notification();
        notification.setId(friendRequestNotify.getId());
        notification.setCorrelationId(friendRequestNotify.getId());
        notification.setRead(false);
        notification.setTimestamp(friendRequestNotify.getCreateTime());
        notification.setType(Notification.Type.FRIEND_REQUEST.getCode());
        notification.setTitle(Notification.Type.FRIEND_REQUEST.getDes());
        if (currentUser.getUid() == friendRequestNotify.getRequesterId()) {
            // 收到自己的好友申请回复
            if (friendRequestNotify.getRequestStatus() == RequestStatus.PENDING) {
                notification.setContent("您申请添加“" + friendRequestNotify.getResponderName() + "”为好友。");
            } else if (friendRequestNotify.getRequestStatus() == RequestStatus.ACCEPTED) {
                notification.setContent("用户“" + friendRequestNotify.getResponderName() + "”已同意您的好友申请。");
            } else if (friendRequestNotify.getRequestStatus() == RequestStatus.REJECTED) {
                notification.setContent("用户“" + friendRequestNotify.getResponderName() + "”已拒绝您的好友申请。");
            }
        } else {
            // 收到其他人的好友申请
            if (friendRequestNotify.getRequestStatus() == RequestStatus.PENDING) {
                notification.setContent("用户“" + friendRequestNotify.getRequesterName() + "”希望添加您为好友。");
            } else if (friendRequestNotify.getRequestStatus() == RequestStatus.ACCEPTED) {
                notification.setContent("您已同意“" + friendRequestNotify.getRequesterName() + "”的好友申请。");
            } else if (friendRequestNotify.getRequestStatus() == RequestStatus.REJECTED) {
                notification.setContent("您已拒绝“" + friendRequestNotify.getRequesterName() + "”的好友申请。");
            }

        }
        return notification;
    }

    public static Message convertToClientMessage(ChatMessage chatMessage) {
        Message message = new Message();
        message.setMessageId(chatMessage.getMessageId());
        message.setSenderId(chatMessage.getSenderId());
        message.setReceiverId(chatMessage.getReceiverId());
        message.setMessageType(chatMessage.getChatMessageTypeValue());
        message.setClientSendTime(chatMessage.getClientSendTime());
        message.setReadStatus(1); // 未读
        message.setClientSeq(chatMessage.getClientSeq());
        message.setServerSeq(chatMessage.getServerSeq());
        message.setClientMsgId(chatMessage.getClientMsgId());
        message.setConversationId(chatMessage.getConversationId());
        message.setConversationType(chatMessage.getConversationTypeValue());
        message.setTimestamp(chatMessage.getTimestamp());
        message.setLocalTemp(false);
        if (chatMessage.getContentCase() == ChatMessage.ContentCase.TEXT_CONTENT) {
            message.setMessageContent(chatMessage.getTextContent().getText());
        } else if (chatMessage.getContentCase() == ChatMessage.ContentCase.IMAGE_CONTENT) {
            message.setMediaInfo(convertToMediaInfo(chatMessage.getImageContent()));
            message.setMessageContent(convertMessageFileJsonContent(message.getMediaInfo()));
        } else if (chatMessage.getContentCase() == ChatMessage.ContentCase.VIDEO_CONTENT) {
            message.setMediaInfo(convertToMediaInfo(chatMessage.getVideoContent()));
            message.setMessageContent(convertMessageFileJsonContent(message.getMediaInfo()));
        } else if (chatMessage.getContentCase() == ChatMessage.ContentCase.FILE_CONTENT) {
            message.setMediaInfo(convertToMediaInfo(chatMessage.getFileContent()));
            message.setMessageContent(convertMessageFileJsonContent(message.getMediaInfo()));
        }


        return message;
    }

    public static ChatMessage convertToIMChatMessage(Message sendMsg, DeviceInfo deviceInfo) {
        ChatMessage.Builder builder = ChatMessage.newBuilder();
        builder.setChatMessageType(ChatMessageType.TEXT);
        builder.setDirection(MessageDirection.REQUEST);
        builder.setClientMsgId(sendMsg.getClientMsgId());
        builder.setClientSendTime(sendMsg.getClientSendTime());
        builder.setClientSeq(sendMsg.getClientSeq());
        builder.setSenderId(sendMsg.getSenderId());
        builder.setReceiverId(sendMsg.getReceiverId());
        builder.setConversationType(ConversationType.forNumber(sendMsg.getConversationType()));
        builder.setConversationId(sendMsg.getConversationId());
        builder.setTraceId(UUID.randomUUID().toString());
        builder.setDeviceInfo(deviceInfo);

        switch (sendMsg.getMessageType()) {
            case com.guru.im.common.constant.MessageType.TEXT:
                builder.setTextContent(buildTextContent(sendMsg));
                builder.setChatMessageType(ChatMessageType.TEXT);
                break;
            case com.guru.im.common.constant.MessageType.IMAGE:
                builder.setImageContent(buildImageContent(sendMsg));
                builder.setChatMessageType(ChatMessageType.IMAGE);
                break;
            case com.guru.im.common.constant.MessageType.VIDEO:
                builder.setVideoContent(buildVideoContent(sendMsg));
                builder.setChatMessageType(ChatMessageType.VIDEO);
                break;
            case com.guru.im.common.constant.MessageType.FILE:
                builder.setFileContent(buildFileContent(sendMsg));
                builder.setChatMessageType(ChatMessageType.FILE);
                break;

        }

        return builder.build();
    }

    private static TextContent buildTextContent(Message sendMsg) {
        return TextContent.newBuilder()
                .setText(sendMsg.getMessageContent())
                .setFormat(TextContent.TextFormat.PLAIN)
                .build();
    }

    private static MediaContent buildMediaContent(MediaInfo mediaInfo) {
        MediaContent.Builder builder = MediaContent.newBuilder();

        builder.setFileId(mediaInfo.getFileId() == null ? 0L : mediaInfo.getFileId());
        builder.setFileName(mediaInfo.getFileName());
        builder.setMimeType(mediaInfo.getMimeType());
        builder.setFileSize(mediaInfo.getFileSize());
        builder.setFileUrl(mediaInfo.getFileUrl());
        if (mediaInfo.getThumbnailUrl() != null) {
            builder.setThumbnailUrl(mediaInfo.getThumbnailUrl());
        }
        return builder.build();
    }

    private static ImageContent buildImageContent(Message sendMsg) {
        MediaInfo mediaInfo = sendMsg.getMediaInfo();
        ImageContent.Builder builder = ImageContent.newBuilder();
        builder.setBase(buildMediaContent(mediaInfo));
        if (mediaInfo.getImageWidth() != null) {
            builder.setWidth(mediaInfo.getImageWidth());
        }
        if (mediaInfo.getImageHeight() != null) {
            builder.setHeight(mediaInfo.getImageHeight());
        }
        builder.setFormat(FileUtil.getImageFormat(mediaInfo.getFileName()));
        return builder.build();
    }

    private static VideoContent buildVideoContent(Message sendMsg) {
        MediaInfo mediaInfo = sendMsg.getMediaInfo();
        VideoContent.Builder builder = VideoContent.newBuilder();
        builder.setBase(buildMediaContent(mediaInfo));
        if (mediaInfo.getImageWidth() != null) {
            builder.setWidth(mediaInfo.getVideoWidth());
        }
        if (mediaInfo.getImageHeight() != null) {
            builder.setHeight(mediaInfo.getVideoHeight());
        }
        if (mediaInfo.getVideoDuration() != null) {
            builder.setDuration(mediaInfo.getVideoDuration());
        }
        if (mediaInfo.getVideoCodec() != null) {
            builder.setVideoCodec(mediaInfo.getVideoCodec());
        }
        if (mediaInfo.getVideoBitrate() != null) {
            builder.setVideoBitrate(mediaInfo.getVideoBitrate());
        }
        if (mediaInfo.getVoiceCodec() != null) {
            builder.setAudioCodec(mediaInfo.getVoiceCodec());
        }
        if (mediaInfo.getVoiceBitrate() != null) {
            builder.setAudioBitrate(mediaInfo.getVoiceBitrate());
        }
        return builder.build();
    }

    private static FileContent buildFileContent(Message sendMsg) {
        MediaInfo mediaInfo = sendMsg.getMediaInfo();
        return FileContent.newBuilder()
                .setBase(buildMediaContent(mediaInfo))
                .setFileType(FileContent.FileType.forNumber(mediaInfo.getFileType()))
                .build();
    }

    private static MediaInfo convertToMediaInfo(ImageContent imageContent) {
        MediaInfo mediaInfo = convertToMediaInfo(imageContent.getBase());
        mediaInfo.setImageWidth(imageContent.getWidth());
        mediaInfo.setImageHeight(imageContent.getHeight());
        mediaInfo.setImageFormat(imageContent.getFormatValue());
        mediaInfo.setFileType(FileUtil.FILE_TYPE_IMAGE);

        return mediaInfo;
    }

    private static MediaInfo convertToMediaInfo(VideoContent videoContent) {
        MediaInfo mediaInfo = convertToMediaInfo(videoContent.getBase());
        mediaInfo.setVideoWidth(videoContent.getWidth());
        mediaInfo.setVideoHeight(videoContent.getHeight());
        mediaInfo.setVoiceBitrate(videoContent.getAudioBitrate());
        mediaInfo.setVoiceCodec(videoContent.getAudioCodec());
        mediaInfo.setVideoBitrate(videoContent.getVideoBitrate());
        mediaInfo.setVideoCodec(videoContent.getVideoCodec());
        mediaInfo.setVideoDuration(videoContent.getDuration());
        mediaInfo.setFileType(FileUtil.FILE_TYPE_VIDEO);

        return mediaInfo;
    }

    private static MediaInfo convertToMediaInfo(FileContent fileContent) {
        MediaInfo mediaInfo = convertToMediaInfo(fileContent.getBase());
        mediaInfo.setFileType(fileContent.getFileTypeValue());
        mediaInfo.setFileType(FileUtil.FILE_TYPE_OTHER);

        return mediaInfo;
    }

    private static MediaInfo convertToMediaInfo(MediaContent mediaContent) {
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setFileId(mediaContent.getFileId());
        mediaInfo.setFileSize(mediaContent.getFileSize());
        mediaInfo.setFileName(mediaContent.getFileName());
        mediaInfo.setMimeType(mediaContent.getMimeType());
        mediaInfo.setFileUrl(mediaContent.getFileUrl());
        mediaInfo.setThumbnailUrl(mediaContent.getThumbnailUrl());
        return mediaInfo;
    }

    private static String convertMessageFileJsonContent(MediaInfo mediaInfo) {
        try {
            return new ObjectMapper().writeValueAsString(mediaInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
