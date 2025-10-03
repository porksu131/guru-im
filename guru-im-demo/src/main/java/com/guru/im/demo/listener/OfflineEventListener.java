package com.guru.im.demo.listener;

import com.google.protobuf.InvalidProtocolBufferException;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.protocol.model.ImMessage;
import com.guru.im.protocol.model.SyncEventResponse;
import com.guru.im.protocol.model.SyncStatus;
import com.guru.im.sdk.event.IMEvent;
import com.guru.im.sdk.event.IMEventListener;
import com.guru.im.sdk.event.IMEventType;

import java.util.List;

public class OfflineEventListener implements IMEventListener {

    private final MainFrame mainFrame;

    public OfflineEventListener(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean canHandle(IMEvent event) {
        return IMEventType.SYNC_EVENT_RESPONSE == event.getType();
    }

    @Override
    public void onEvent(IMEvent event) {
        ImMessage imMessage = event.getImMessage();
        SyncEventResponse syncResponse = imMessage.getSyncEventResponse();
        if (syncResponse.getSyncStatus() != SyncStatus.SYNC_STATUS_FAILED) {
            // 服务端返回同步成功
            List<ImMessage> imEvents = getImMessages(syncResponse);
            for (ImMessage imEvent : imEvents) {
                if (imEvent.getBodyCase() == ImMessage.BodyCase.FRIEND_REQUEST) {
                    mainFrame.onReceiveFriendRequestNotify(imEvent.getFriendRequest());
                } else if (imEvent.getBodyCase() == ImMessage.BodyCase.READ_RECEIPT_NOTIFY) {
                    mainFrame.onReceiveReadReceiptNotify(imEvent.getReadReceiptNotify());
                }
            }
            // 保存最大同步序列号到本地
            mainFrame.saveLastSyncSeq(syncResponse.getLatestSequence());

            // 如果还有数据，重新读取最大同步序列号，再次发起同步
            if (syncResponse.getHasMore()) {
                mainFrame.sendSyncEventRequest();
            }
            return;
        }

        //mainFrame.showError(syncResponse.getErrorMsg());
    }


    private List<ImMessage> getImMessages(SyncEventResponse syncResponse) {
        return syncResponse.getEventsList().stream().map(obj -> {
            try {
                return ImMessage.parseFrom(obj.toByteArray());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
