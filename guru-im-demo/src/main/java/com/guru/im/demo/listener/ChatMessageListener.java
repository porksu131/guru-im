package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

import java.util.Collections;

public class ChatMessageListener implements IMEventListener {
    private final MainFrame mainFrame;

    public ChatMessageListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.CHAT_MESSAGE_RECEIVED == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        ChatMessage chatMessage = event.getImMessage().getChatMessage();
        this.mainFrame.onReceiveChatMessage(Collections.singletonList(chatMessage));
    }
}
