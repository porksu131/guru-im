package com.guru.im.file.model.vo;

import com.guru.im.file.model.pojo.FileInfo;
import com.guru.im.file.model.pojo.FilePartInfo;

import java.util.List;

public class UploadInitResponse {
    private FileInfo fileInfo;
    private List<FilePartInfo> partInfos;// 分片号 -> URL
    public UploadInitResponse() {
    }

    public UploadInitResponse(FileInfo fileInfo, List<FilePartInfo> partInfos) {
        this.fileInfo = fileInfo;
        this.partInfos = partInfos;
    }


    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public List<FilePartInfo> getPartInfos() {
        return partInfos;
    }

    public void setPartInfos(List<FilePartInfo> partInfos) {
        this.partInfos = partInfos;
    }
}