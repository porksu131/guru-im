package com.guru.im.demo.listener;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.ReadReceiptNotify;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

public class ReadReceiptListener implements IMEventListener {
    private final MainFrame mainFrame;

    public ReadReceiptListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.READ_RECEIPT == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        ReadReceiptNotify readReceiptNotify = event.getImMessage().getReadReceiptNotify();
        this.mainFrame.onReceiveReadReceiptNotify(readReceiptNotify);
    }
}
