package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.SignalingMessage;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class SignalMessageListener implements IMEventListener {
    private final MainFrame mainFrame;

    public SignalMessageListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.SIGNALING_MESSAGE == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        SignalingMessage signalingMessage = event.getImMessage().getSignalingMessage();
        this.mainFrame.onReceiveSignalingMessage(signalingMessage);
    }
}
