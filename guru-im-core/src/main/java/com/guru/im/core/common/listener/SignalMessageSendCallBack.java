package com.guru.im.core.common.listener;

import com.guru.im.protocol.model.SignalMessageAck;

public interface SignalMessageSendCallBack {
    void onSendSuccess(SignalMessageAck signalMessageAck);

    void onSendFail(String errorMsg);
}
