package com.guru.im.sdk.service;

import com.guru.im.sdk.IMClientManager;
import com.guru.im.sdk.service.impl.ChatServiceImpl;
import com.guru.im.sdk.service.impl.GroupServiceImpl;
import com.guru.im.sdk.service.impl.SignalServiceImpl;
import com.guru.im.sdk.service.impl.UserServiceImpl;

public class ServiceFactory {
    private final ChatService chatService;
    private final GroupService groupService;
    private final UserService userService;
    private final SignalService signalService;
    
    public ServiceFactory(IMClientManager imClientManager) {
        this.chatService = new ChatServiceImpl(imClientManager);
        this.groupService = new GroupServiceImpl(imClientManager);
        this.userService = new UserServiceImpl(imClientManager);
        this.signalService = new SignalServiceImpl(imClientManager);
    }

    public ChatService getChatService() {
        return chatService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public UserService getUserService() {
        return userService;
    }

    public SignalService getSignalService() {
        return signalService;
    }
}