package com.guru.im.demo.gui.chat.listener;

import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.gui.file.FileTransferManager;
import com.guru.im.demo.gui.file.listener.UploadProgressCallback;
import com.guru.im.demo.gui.file.model.FileTransfer;

public class FileUploadProgressListener implements UploadProgressCallback {
    private ChatPanel chatPanel;

    public FileUploadProgressListener(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
    }

    @Override
    public void onCompletePrepareTransfer(FileTransfer transfer) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        this.chatPanel.handleCompletePrepareTransfer(transfer);
    }

    @Override
    public void onCompleteGenerateThumbnail(FileTransfer transfer) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        this.chatPanel.handleCompleteGenerateThumbnail(transfer);
    }

    @Override
    public void onProgress(FileTransfer transfer, int progress) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        int newProgress = progress;
        if (transfer.getCompleteThumbnail() != null && transfer.getCompleteThumbnail()) {
            int thumbnailProgress = FileTransferManager.THUMBNAIL_PROGRESS;
            newProgress = thumbnailProgress + (100 - thumbnailProgress) * (progress / 100);
        }
        if (newProgress == 100) {
            transfer.setProgress(99);
        } else {
            transfer.setProgress(newProgress);
        }

        this.chatPanel.handleFileUploadProgressChanged(transfer);
    }

    @Override
    public void onComplete(FileTransfer transfer) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        transfer.setProgress(100);
        this.chatPanel.handleFileUploadComplete(transfer);
    }

    @Override
    public void onError(FileTransfer transfer, String error) {
        if (transfer.isCancelled() || transfer.isPaused()) {
            return;
        }
        this.chatPanel.handleUploadFileError(transfer, error);
    }
}
