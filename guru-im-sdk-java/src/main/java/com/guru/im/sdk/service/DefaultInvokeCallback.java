package com.guru.im.sdk.service;

import com.guru.im.core.common.constant.ResponseCode;
import com.guru.im.core.common.listener.InvokeCallback;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.Response;

public class DefaultInvokeCallback implements InvokeCallback {
    private final MessageSendCallback messageSendCallback;

    public DefaultInvokeCallback(MessageSendCallback messageSendCallback) {
        this.messageSendCallback = messageSendCallback;
    }

    @Override
    public void operationSucceed(final ImMessage imMessage) {
        if (this.messageSendCallback != null) {
            Response response = imMessage.getResponse();
            if (imMessage.getResponse().getCode() == ResponseCode.SUCCESS) {
                messageSendCallback.onSuccess();
            } else {
                messageSendCallback.onFailure(response.getMsg());
            }
        }
    }

    @Override
    public void operationFail(final Throwable throwable) {
        if (this.messageSendCallback != null) {
            messageSendCallback.onFailure(throwable.getMessage());
        }
    }
}
