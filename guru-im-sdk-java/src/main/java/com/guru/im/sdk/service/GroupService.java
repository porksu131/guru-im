package com.guru.im.sdk.service;

import java.util.List;

public interface GroupService {
    void createGroup(String name, List<Long> memberIds);
    void sendGroupMessage(long groupId, String text);
    void addGroupMembers(long groupId, List<Long> memberIds);
    void removeGroupMembers(long groupId, List<Long> memberIds);
    // 其他群组相关方法...
}