package com.guru.im.demo.gui.file.listener;

import com.guru.im.demo.gui.file.model.FileTransfer;

public interface DownloadProgressCallback {
    void onProgress(FileTransfer transfer, int progress, double speed);

    void onComplete(FileTransfer transfer);

    default void onError(FileTransfer transfer, String error) {
    }
}