package com.guru.im.protocol.util;

import com.guru.im.protocol.model.*;

public class MessageHelper {
    public static long getToUserId(ImMessage imMessage) {
        String toUserId = imMessage.getExtraFieldsMap().get("toUserId");
        if (toUserId == null) {
            throw new RuntimeException("toUserId is null");
        }
        return Long.parseLong(toUserId);
    }

    public static String getToDeviceId(ImMessage imMessage) {
        String toDeviceId = imMessage.getExtraFieldsMap().get("toDeviceId");
        if (toDeviceId == null) {
            throw new RuntimeException("toDeviceId is null");
        }
        return toDeviceId;
    }
}
