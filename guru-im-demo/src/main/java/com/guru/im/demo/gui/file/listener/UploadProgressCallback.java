package com.guru.im.demo.gui.file.listener;

import com.guru.im.demo.gui.file.model.FileTransfer;

public interface UploadProgressCallback {
    default void onCompletePrepareTransfer(FileTransfer fileTransfer) {

    }
    default void onCompleteGenerateThumbnail(FileTransfer transfer) {
    }

    default void onProgress(FileTransfer transfer, int progress) {
    }

    void onComplete(FileTransfer transfer);

    default void onError(FileTransfer transfer, String error) {
    }
}