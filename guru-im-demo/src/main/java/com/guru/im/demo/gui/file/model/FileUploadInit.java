package com.guru.im.demo.gui.file.model;

import java.util.List;

public class FileUploadInit {
    private FileUploadInfo fileInfo;
    private List<FileUploadPartInfo> partInfos;// 分片号 -> URL

    public FileUploadInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileUploadInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public List<FileUploadPartInfo> getPartInfos() {
        return partInfos;
    }

    public void setPartInfos(List<FileUploadPartInfo> partInfos) {
        this.partInfos = partInfos;
    }
}
