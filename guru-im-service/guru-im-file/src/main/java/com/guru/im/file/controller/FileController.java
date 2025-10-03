package com.guru.im.file.controller;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.file.model.pojo.FileInfo;
import com.guru.im.file.model.vo.UploadInitRequest;
import com.guru.im.file.model.vo.UploadInitResponse;
import com.guru.im.file.service.FileService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 检查文件是否已存在(秒传)
     */
    @GetMapping("/check")
    public ResponseResult<FileInfo> checkFileExist(@RequestParam String md5) {
        FileInfo fileInfo = fileService.checkFileExist(md5);
        return ResponseResult.ok(fileInfo);
    }

    /**
     * 初始化文件上传
     */
    @PostMapping("/initUpload")
    public ResponseResult<UploadInitResponse> initFileUpload(@RequestBody UploadInitRequest uploadInitRequest) {
        return fileService.initFileUpload(uploadInitRequest);
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/completePart")
    public ResponseResult<Boolean> completePartUpload(@RequestBody Map<String, String> params) {
        Long fileId = params.get("fileId") == null ? 0L : Long.parseLong(params.get("fileId"));
        Integer partNumber = params.get("partNumber") == null ? 1 : Integer.parseInt(params.get("partNumber"));
        String partMd5 = params.get("partMd5") == null ? "" : params.get("partMd5");
        boolean success = fileService.completePartUpload(fileId, partNumber, partMd5);
        return ResponseResult.ok(success);
    }

    /**
     * 完成文件上传
     */
    @PostMapping("/complete")
    public ResponseResult<FileInfo> completeFileUpload(@RequestBody Map<String, String> params) {
        Long fileId = params.get("fileId") == null ? 0L : Long.parseLong(params.get("fileId"));
        return fileService.completeFileUpload(fileId);
    }

    /**
     * 获取分片上传URL
     */
    @GetMapping("/uploadUrl")
    public ResponseResult<String> getUploadUrl(@RequestBody Map<String, String> params) {
        Long fileId = params.get("fileId") == null ? 0L : Long.parseLong(params.get("fileId"));
        Integer partNumber = params.get("partNumber") == null ? 1 : Integer.parseInt(params.get("partNumber"));
        String uploadUrl = fileService.getUploadUrl(fileId, partNumber);
        if (uploadUrl == null) {
            return ResponseResult.fail("获取上传URL失败");
        }
        return ResponseResult.ok(uploadUrl);
    }

    /**
     * 获取下载URL
     */
    @GetMapping("/downloadUrl")
    public ResponseResult<String> getDownloadUrl(@RequestParam Long fileId) {
        String downloadUrl = fileService.getDownloadUrl(fileId);
        if (downloadUrl == null) {
            return ResponseResult.fail("获取下载URL失败");
        }
        return ResponseResult.ok(downloadUrl);
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/info")
    public ResponseResult<FileInfo> getFileInfo(@RequestParam Long fileId) {
        FileInfo fileInfo = fileService.getFileInfo(fileId);
        return ResponseResult.ok(fileInfo);
    }
}