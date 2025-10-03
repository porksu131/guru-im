package com.guru.im.demo.gui.chat.listener;

import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.gui.file.listener.DownloadProgressCallback;
import com.guru.im.demo.gui.file.model.FileTransfer;

public class FileDownloadProgressListener implements DownloadProgressCallback {
    private ChatPanel chatPanel;

    public FileDownloadProgressListener(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
    }

    @Override
    public void onProgress(FileTransfer transfer, int progress, double speed) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        this.chatPanel.handleFileDownloadProgressChanged(transfer, progress, speed);
    }

    @Override
    public void onComplete(FileTransfer transfer) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        this.chatPanel.handleFileDownloadComplete(transfer);
    }

    @Override
    public void onError(FileTransfer transfer, String error) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        this.chatPanel.handleFileDownloadFailed(transfer, error);
    }
}
