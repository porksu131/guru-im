package com.guru.im.demo.signal.model;

import com.guru.im.demo.model.DeviceInfo;
import com.guru.im.demo.model.UserInfo;

import java.util.Map;

/**
 * 媒体相关数据类
 */
public class MediaDataClasses {
    /**
     * 统一的媒体初始化数据基类
     */
    public static abstract class MediaInitData {
        public String sessionId;
        public UserInfo currentUser;
        public DeviceInfo deviceInfo;
        public boolean isIncoming;

        public MediaInitData(long sessionId, UserInfo currentUser,
                             DeviceInfo deviceInfo, boolean isIncoming) {
            this.sessionId = String.valueOf(sessionId);
            this.currentUser = currentUser;
            this.deviceInfo = deviceInfo;
            this.isIncoming = isIncoming;
        }
    }

    /**
     * 通话初始化数据
     */
    public static class CallInitData extends MediaInitData {
        public String targetUserId;
        public String targetUserName;
        public boolean isVideoCall;

        public CallInitData(long sessionId, long targetUserId, String targetUserName,
                            boolean isIncoming, UserInfo currentUser,
                            DeviceInfo deviceInfo) {
            super(sessionId, currentUser, deviceInfo, isIncoming);
            this.targetUserId = String.valueOf(targetUserId);
            this.targetUserName = targetUserName;
        }
    }

    /**
     * 会议初始化数据
     */
    public static class ConferenceInitData extends MediaInitData {
        public String conferenceTitle;
        public long hostUser;
        public Map<Long, String> participants;

        public ConferenceInitData(long conferenceId, String conferenceTitle, long hostUser,
                                  UserInfo currentUser, DeviceInfo deviceInfo,
                                  Map<Long, String> participants, boolean isIncoming) {
            super(conferenceId, currentUser, deviceInfo, isIncoming);
            this.conferenceTitle = conferenceTitle;
            this.hostUser = hostUser;
            this.participants = participants;
        }
    }
}