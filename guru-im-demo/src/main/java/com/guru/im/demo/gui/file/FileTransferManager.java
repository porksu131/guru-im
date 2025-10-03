package com.guru.im.demo.gui.file;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.file.listener.DownloadProgressCallback;
import com.guru.im.demo.gui.file.listener.UploadProgressCallback;
import com.guru.im.demo.gui.file.model.*;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.demo.sqlite.DatabaseManager;
import com.guru.im.demo.util.AppUtil;
import com.guru.im.demo.util.FileUtil;
import com.guru.im.demo.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileTransferManager {
    private UserInfo currentUser;
    private ExecutorService executor;
    private DatabaseManager dbManager;
    private final int MAX_CONCURRENT_PARTS = 4; // 最大并行分片数
    public static final int THUMBNAIL_PROGRESS = 10; // 缩略图占用整个上传的百分之10

    public FileTransferManager(UserInfo currentUser, DatabaseManager dbManager) {
        this.currentUser = currentUser;
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_PARTS);
        this.dbManager = dbManager;
    }

    public void initFileTransfer(FileTransfer transfer, UploadProgressCallback progressCallback) {
        File file = new File(transfer.getFilePath());
        transfer.setMd5(calculateMD5(file));
        try {
            transfer.setContentType(Files.probeContentType(file.toPath()));
        } catch (IOException e) {
            transfer.setContentType("application/octet-stream");
        }
        progressCallback.onCompletePrepareTransfer(transfer);
    }

    public Future asyncGenerateThumbnail(FileTransfer transfer, UploadProgressCallback callback) {
        return this.executor.submit(() -> {
            try {
                // 生成缩略图
                String localImageThumbnailPath = generateLocalImageThumbnail(transfer);
                transfer.setThumbnailPath(localImageThumbnailPath);
                callback.onCompleteGenerateThumbnail(transfer);
            } catch (Exception e) {
                callback.onError(transfer, e.getMessage());
            }
        });
    }

    public void startUpload(FileTransfer transfer, UploadProgressCallback progressCallback) {
        executor.execute(() -> {
            try {
                // 初始化文件上传信息
                initFileTransfer(transfer, progressCallback);

                // 图片和视频需要生成缩略图，并上传缩略图，之后才上传原文件
                if (transfer.getFileType() == FileUtil.FILE_TYPE_IMAGE
                        || transfer.getFileType() == FileUtil.FILE_TYPE_VIDEO) {
                    uploadFileWithThumbnail(transfer, progressCallback);
                    return;
                }

                uploadFile(transfer, progressCallback);

            } catch (Exception e) {
                transfer.setStatus(FileTransfer.Status.FAILED);
                progressCallback.onError(transfer, e.getMessage());
            }
        });
    }

    private void uploadFileWithThumbnail(FileTransfer transfer, UploadProgressCallback progressCallback) throws IOException {
        // 如果缩略图已经上传完成，直接上传原文件
        if (Boolean.TRUE.equals(transfer.getCompleteThumbnail()) && transfer.getThumbnailUrl() != null) {
            uploadFile(transfer, progressCallback);
            return;
        }

        // 如果缩略图URL已存在，说明之前已经上传过缩略图
        if (transfer.getThumbnailUrl() != null) {
            transfer.setCompleteThumbnail(true);
            uploadFile(transfer, progressCallback);
            return;
        }

        // 生成缩略图
        if (transfer.getThumbnailPath() == null || !new File(transfer.getThumbnailPath()).exists()) {
            try {
                Future future = asyncGenerateThumbnail(transfer, progressCallback);
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        // 先上传缩略图，再上传源文件
        File thumbnailFile = new File(transfer.getThumbnailPath());
        if (!thumbnailFile.exists()) {
            // 无法生成缩略图，直接上传原文件
            uploadFile(transfer, progressCallback);
            return;
        }

        FileTransfer thumbnailTransfer = new FileTransfer(transfer.getFileType(), thumbnailFile);
        thumbnailTransfer.setMd5(calculateMD5(thumbnailFile));
        thumbnailTransfer.setContentType("image/jpeg");

        // 设置缩略图传输的相关信息，便于关联
        thumbnailTransfer.setClientMsgId(transfer.getClientMsgId());
        thumbnailTransfer.setConversationId(transfer.getConversationId());

        uploadFile(thumbnailTransfer, new UploadProgressCallback() {
            @Override
            public void onComplete(FileTransfer thumbnailTransfer) {
                transfer.setCompleteThumbnail(true);
                transfer.setThumbnailUrl(thumbnailTransfer.getFileUrl());
                safeUpdateProgress(transfer, THUMBNAIL_PROGRESS, progressCallback);
                boolean delete = thumbnailFile.delete(); // 上传完成后，删除本地临时缩略图文件

                try {
                    uploadFile(transfer, progressCallback);
                } catch (IOException e) {
                    transfer.setStatus(FileTransfer.Status.FAILED);
                    progressCallback.onError(transfer, e.getMessage());
                }
            }

            @Override
            public void onError(FileTransfer thumbnailTransfer, String error) {
                transfer.setStatus(FileTransfer.Status.FAILED);
                progressCallback.onError(transfer, "缩略图上传失败: " + error);
            }
        });
    }

    private void uploadFile(FileTransfer transfer, UploadProgressCallback progressCallback) throws IOException {
        // 初始化上传
        ResponseResult<FileUploadInit> initRes = callInitUpload(transfer);
        if (initRes.getCode() != ResponseResult.SUCCESS) {
            throw new RuntimeException("initUpload failed: " + initRes.getMsg());
        }

        FileUploadInit initResponse = initRes.getData();
        transfer.setFileId(initResponse.getFileInfo().getId());

        if (initResponse.getFileInfo().getStatus() == 1) {
            // 秒传成功
            completeUpload(transfer, initResponse.getFileInfo().getDownloadUrl(), progressCallback);
            return;
        }

        // 开始并行分片上传
        uploadPartsInParallel(transfer, initResponse, progressCallback);

        // 完成上传
        completeUpload(transfer, progressCallback);
    }

    private void completeUpload(FileTransfer transfer, UploadProgressCallback processCallback) {
        ResponseResult<FileUploadComplete> completeRes = ApiService.completeFileUpload(transfer.getFileId(), currentUser.getAccessToken());
        if (completeRes.getCode() != ResponseResult.SUCCESS) {
            transfer.setStatus(FileTransfer.Status.FAILED);
            processCallback.onError(transfer, completeRes.getMsg());
            return;
        }

        completeUpload(transfer, completeRes.getData().getDownloadUrl(), processCallback);
    }

    private void completeUpload(FileTransfer transfer, String downloadUrl, UploadProgressCallback processCallback) {
        transfer.setProgress(100);
        transfer.setFileUrl(downloadUrl);
        transfer.trySetStatus(FileTransfer.Status.COMPLETED);

        processCallback.onComplete(transfer);
    }

    private ResponseResult<FileUploadInit> callInitUpload(FileTransfer transfer) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("originalName", transfer.getFileName());
        requestBody.put("md5", transfer.getMd5());
        requestBody.put("size", transfer.getFileSize());
        requestBody.put("contentType", transfer.getContentType());
        requestBody.put("userId", currentUser.getUid());
        requestBody.put("partSize", 10 * 1024 * 1024);
        return ApiService.initFileUpload(requestBody, currentUser.getAccessToken());
    }

    private void uploadPartsInParallel(FileTransfer transfer, FileUploadInit initResponse, UploadProgressCallback callback) {
        List<FileUploadPartInfo> partInfos = initResponse.getPartInfos();
        int totalParts = partInfos.size();

        // 统计已完成和未完成的分片
        List<FileUploadPartInfo> partsToUpload = new ArrayList<>();
        int completedParts = 0;

        for (FileUploadPartInfo partInfo : partInfos) {
            if (partInfo.getStatus() == 1) {
                completedParts++;
            } else {
                partsToUpload.add(partInfo);
            }
        }

        // 初始化进度
        int initialProgress = totalParts > 0 ? (completedParts * 100 / totalParts) : 0;
        safeUpdateProgress(transfer, initialProgress, callback);

        if (partsToUpload.isEmpty()) {
            return;
        }

        // 根据需要上传的分片数量选择上传策略
        if (partsToUpload.size() == 1) {
            uploadSinglePartSequentially(transfer, partsToUpload.get(0), completedParts, totalParts, callback);
        } else {
            uploadMultiplePartsInParallel(transfer, partsToUpload, completedParts, totalParts, callback);
        }
    }

    /**
     * 单分片顺序上传
     */
    private void uploadSinglePartSequentially(FileTransfer transfer, FileUploadPartInfo partInfo,
                                              int completedParts, int totalParts, UploadProgressCallback callback) {
        try (RandomAccessFile file = new RandomAccessFile(new File(transfer.getFilePath()), "r")) {
            long fileSize = file.length();

            uploadSinglePart(transfer, file, partInfo, fileSize);

            // 重新打开文件计算MD5，确保文件指针正确
            try (RandomAccessFile md5File = new RandomAccessFile(new File(transfer.getFilePath()), "r")) {
                String partMd5 = calculatePartMD5(md5File,
                        (partInfo.getPartNumber() - 1) * partInfo.getPartSize(),
                        calculateActualPartSize(partInfo, fileSize));

                ResponseResult<Boolean> result = ApiService.completePart(
                        transfer.getFileId(), partInfo.getPartNumber(),
                        partMd5, currentUser.getAccessToken());

                if (result.getCode() == ResponseResult.SUCCESS) {
                    int newCompleted = completedParts + 1;
                    int progress = totalParts > 0 ? (newCompleted * 100 / totalParts) : 100;
                    safeUpdateProgress(transfer, progress, callback);
                } else {
                    throw new RuntimeException("Complete part failed: " + result.getMsg());
                }
            }

        } catch (Exception e) {
            callback.onError(transfer, "Part " + partInfo.getPartNumber() + " upload failed: " + e.getMessage());
            throw new RuntimeException("Sequential upload failed", e);
        }
    }

    /**
     * 多分片并行上传 - 修复多线程问题
     */
    private void uploadMultiplePartsInParallel(FileTransfer transfer, List<FileUploadPartInfo> partsToUpload,
                                               int completedParts, int totalParts, UploadProgressCallback callback) {
        int threadPoolSize = Math.min(partsToUpload.size(), MAX_CONCURRENT_PARTS);
        ExecutorService partExecutor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(partsToUpload.size());
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger currentCompletedParts = new AtomicInteger(completedParts);

        try {
            long fileSize = new File(transfer.getFilePath()).length();

            for (FileUploadPartInfo partInfo : partsToUpload) {
                partExecutor.submit(() -> {
                    // 每个分片使用独立的RandomAccessFile实例，避免多线程竞争
                    try (RandomAccessFile file = new RandomAccessFile(new File(transfer.getFilePath()), "r")) {
                        if (transfer.isCancelled()) {
                            return;
                        }

                        if (transfer.isPaused()) {
                            while (transfer.isPaused() && !transfer.isCancelled()) {
                                Thread.sleep(1000);
                            }
                            if (transfer.isCancelled()) {
                                return;
                            }
                        }

                        uploadSinglePart(transfer, file, partInfo, fileSize);

                        // 重新打开文件计算MD5
                        try (RandomAccessFile md5File = new RandomAccessFile(new File(transfer.getFilePath()), "r")) {
                            String partMd5 = calculatePartMD5(md5File,
                                    (partInfo.getPartNumber() - 1) * partInfo.getPartSize(),
                                    calculateActualPartSize(partInfo, fileSize));

                            ResponseResult<Boolean> result = ApiService.completePart(
                                    transfer.getFileId(), partInfo.getPartNumber(),
                                    partMd5, currentUser.getAccessToken());

                            if (result.getCode() == ResponseResult.SUCCESS) {
                                int newCompleted = currentCompletedParts.incrementAndGet();
                                int progress = totalParts > 0 ? (newCompleted * 100 / totalParts) : 0;
                                safeUpdateProgress(transfer, progress, callback);
                            } else {
                                errorCount.incrementAndGet();
                                throw new RuntimeException("Complete part failed: " + result.getMsg());
                            }
                        }

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        callback.onError(transfer, "Part " + partInfo.getPartNumber() + " upload failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();

            if (errorCount.get() > 0) {
                throw new RuntimeException(errorCount.get() + " parts failed to upload");
            }

        } catch (Exception e) {
            throw new RuntimeException("Parallel upload failed", e);
        } finally {
            partExecutor.shutdown();
        }
    }

    /**
     * 单分片上传 - 使用验证过的工作方式
     */
    private void uploadSinglePart(FileTransfer transfer, RandomAccessFile file,
                                  FileUploadPartInfo partInfo, long fileSize) throws Exception {
        long start = (partInfo.getPartNumber() - 1) * partInfo.getPartSize();
        long actualPartSize = calculateActualPartSize(partInfo, fileSize);

        // 验证分片范围
        if (start >= fileSize) {
            throw new IllegalArgumentException("Part start beyond file size");
        }
        if (actualPartSize <= 0) {
            throw new IllegalArgumentException("Invalid part size: " + actualPartSize);
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(partInfo.getPreSignedUrl()).openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(actualPartSize));
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            file.seek(start);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] buffer = new byte[8192];
                long totalRead = 0;
                long remaining = actualPartSize;

                while (remaining > 0 && !transfer.isCancelled() && !transfer.isPaused()) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = file.read(buffer, 0, toRead);
                    
                    if (read == -1) {
                        throw new IOException("File ended prematurely. Expected: " + actualPartSize + ", Read: " + totalRead);
                    }
                    
                    os.write(buffer, 0, read);
                    totalRead += read;
                    remaining -= read;
                }

                if (transfer.isCancelled() || transfer.isPaused()) {
                    throw new InterruptedException("Transfer interrupted");
                }

                // 验证完整性
                if (totalRead != actualPartSize) {
                    throw new IOException(String.format(
                        "Part upload incomplete. Expected: %d, Actual: %d", 
                        actualPartSize, totalRead));
                }
                
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String errorMessage = "Upload part failed with code: " + responseCode;
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        errorMessage += ", Response: " + new String(errorStream.readAllBytes());
                    }
                }
                throw new RuntimeException(errorMessage);
            }

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private long calculateActualPartSize(FileUploadPartInfo partInfo, long fileSize) {
        long start = (partInfo.getPartNumber() - 1) * partInfo.getPartSize();
        return Math.min(partInfo.getPartSize(), fileSize - start);
    }

    public String calculateMD5(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String calculatePartMD5(RandomAccessFile file, long start, long size) throws Exception {
        file.seek(start);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        long totalRead = 0;
        long remaining = size;

        while (remaining > 0) {
            int toRead = (int) Math.min(buffer.length, remaining);
            int read = file.read(buffer, 0, toRead);
            if (read == -1) {
                throw new IOException("MD5 calculation incomplete. Expected: " + size + ", Read: " + totalRead);
            }
            digest.update(buffer, 0, read);
            totalRead += read;
            remaining -= read;
        }

        if (totalRead != size) {
            throw new IOException("MD5 calculation failed. Expected: " + size + ", Actual: " + totalRead);
        }

        byte[] md5Bytes = digest.digest();
        return Base64.getEncoder().encodeToString(md5Bytes);
    }

    public void pauseUpload(FileTransfer transfer) {
        transfer.setPaused(true);
        transfer.trySetStatus(FileTransfer.Status.PAUSED);
        dbManager.saveFileTransfer(transfer);
    }

    public void resumeUpload(FileTransfer transfer, UploadProgressCallback progressCallback) {
        transfer.setPaused(false);
        transfer.trySetStatus(FileTransfer.Status.IN_PROGRESS);
        dbManager.saveFileTransfer(transfer);
        startUpload(transfer, progressCallback);
    }

    public void cancelUpload(FileTransfer transfer) {
        transfer.setCancelled(true);
        transfer.trySetStatus(FileTransfer.Status.CANCELLED);
        dbManager.saveFileTransfer(transfer);
    }

    private String generateLocalImageThumbnail(FileTransfer transfer) {
        String thumbnailDir = Path.of(AppUtil.getAppDataDir(), "thumbnail", String.valueOf(currentUser.getUid())).toString();
        if (!new File(thumbnailDir).exists()) {
            new File(thumbnailDir).mkdirs();
        }

        String thumbnailPath = thumbnailDir + File.separator +
                FileUtil.getFileMainName(transfer.getFileName()) + "_thumbnail_" + transfer.getMd5() + ".jpg";
        if (new File(thumbnailPath).exists()) {
            return thumbnailPath;
        }

        if (transfer.getFileType() == FileUtil.FILE_TYPE_IMAGE) {
            Dimension imageDimension = ImageUtil.getImageDimension(transfer.getFilePath());
            if (imageDimension != null) {
                transfer.setOriginWidth((int) imageDimension.getWidth());
                transfer.setOriginHeight((int) imageDimension.getHeight());
            }
            boolean success = ImageUtil.generateImageThumbnail(transfer.getFilePath(), thumbnailPath, 600, 400);
            return success ? thumbnailPath : null;
        } else if (transfer.getFileType() == FileUtil.FILE_TYPE_VIDEO) {
            GrabberVideoInfo grabberVideoInfo = ImageUtil.generateVideoImage(transfer.getFilePath());

            if (grabberVideoInfo != null) {
                transfer.setOriginWidth(grabberVideoInfo.getImageWidth());
                transfer.setOriginHeight(grabberVideoInfo.getImageHeight());
                transfer.setDuration(grabberVideoInfo.getDuration());
                transfer.setAudioCodec(grabberVideoInfo.getAudioCodec());
                transfer.setVideoCodec(grabberVideoInfo.getVideoCodec());
                transfer.setAudioBitrate(grabberVideoInfo.getAudioBitrate());
                transfer.setVideoBitrate(grabberVideoInfo.getVideoBitrate());
                boolean success = ImageUtil.generateVideoThumbnail(grabberVideoInfo.getScreenshot(), thumbnailPath, 600, 400);
                return success ? thumbnailPath : null;
            }
        }
        return null;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void startDownload(FileTransfer transfer, DownloadProgressCallback callback) {
        executor.submit(() -> {
            try {
                downloadFile(transfer, callback);
            } catch (Exception e) {
                transfer.setStatus(FileTransfer.Status.FAILED);
                dbManager.saveFileTransfer(transfer);
                callback.onError(transfer, e.getMessage());
            }
        });
    }

    private void downloadFile(FileTransfer transfer, DownloadProgressCallback callback) throws IOException {
        File outputFile = new File(transfer.getFileSavePath());
        if (outputFile.exists() && outputFile.length() == transfer.getFileSize()) {
            transfer.setProgress(100);
            transfer.trySetStatus(FileTransfer.Status.COMPLETED);
            dbManager.saveFileTransfer(transfer);
            callback.onComplete(transfer);
            return;
        }

        HttpURLConnection conn = null;
        RandomAccessFile raf = null;
        InputStream is = null;

        try {
            URL url = new URL(transfer.getFileUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            long existingLength = 0;
            if (outputFile.exists()) {
                existingLength = outputFile.length();
                if (existingLength > 0) {
                    conn.setRequestProperty("Range", "bytes=" + existingLength + "-");
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK &&
                    responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new IOException("Server returned HTTP code: " + responseCode);
            }

            long contentLength = conn.getContentLengthLong();
            if (contentLength == -1) {
                contentLength = transfer.getFileSize() - existingLength;
            } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                contentLength += existingLength;
            }

            long totalBytesToDownload = contentLength;
            long totalFileSize = transfer.getFileSize();

            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            raf = new RandomAccessFile(outputFile, "rw");
            raf.seek(existingLength);

            is = conn.getInputStream();
            byte[] buffer = new byte[8192];
            int read;
            long totalRead = existingLength;
            long startTime = System.currentTimeMillis();
            long lastUpdateTime = startTime;
            long lastUpdateBytes = totalRead;

            while ((read = is.read(buffer)) != -1) {
                if (transfer.isPaused() || transfer.isCancelled()) {
                    break;
                }

                raf.write(buffer, 0, read);
                totalRead += read;

                long currentTime = System.currentTimeMillis();
                long timeElapsed = currentTime - lastUpdateTime;

                if (timeElapsed > 100) {
                    double speed = calculateDownloadSpeed(lastUpdateBytes, totalRead, lastUpdateTime, currentTime);
                    int progress = (int) ((totalRead * 100) / totalFileSize);
                    transfer.setProgress(progress);
                    callback.onProgress(transfer, progress, speed);
                    lastUpdateTime = currentTime;
                    lastUpdateBytes = totalRead;
                }
            }

            if (totalRead == totalFileSize) {
                transfer.setProgress(100);
                transfer.trySetStatus(FileTransfer.Status.COMPLETED);
                callback.onComplete(transfer);
            } else if (transfer.isPaused()) {
                transfer.setStatus(FileTransfer.Status.PAUSED);
            } else if (transfer.isCancelled()) {
                transfer.setStatus(FileTransfer.Status.CANCELLED);
                if (outputFile.exists()) {
                    outputFile.delete();
                }
            } else {
                transfer.setStatus(FileTransfer.Status.FAILED);
            }

        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {}
            try {
                if (raf != null) raf.close();
            } catch (IOException e) {}
            if (conn != null) conn.disconnect();
            dbManager.saveFileTransfer(transfer);
        }
    }

    private double calculateDownloadSpeed(long startBytes, long endBytes, long startTime, long endTime) {
        long bytesTransferred = endBytes - startBytes;
        long timeElapsed = endTime - startTime;
        if (timeElapsed == 0) return 0;
        return (bytesTransferred * 1000.0) / timeElapsed;
    }

    public void resumeDownload(FileTransfer transfer, DownloadProgressCallback callback) {
        transfer.setPaused(false);
        transfer.trySetStatus(FileTransfer.Status.IN_PROGRESS);
        dbManager.saveFileTransfer(transfer);
        startDownload(transfer, callback);
    }

    public void pauseDownload(FileTransfer transfer) {
        transfer.setPaused(true);
        transfer.trySetStatus(FileTransfer.Status.PAUSED);
        dbManager.saveFileTransfer(transfer);
    }

    public void cancelDownload(FileTransfer transfer) {
        transfer.setCancelled(true);
        transfer.trySetStatus(FileTransfer.Status.CANCELLED);
        dbManager.saveFileTransfer(transfer);
        File partialFile = new File(transfer.getFilePath());
        if (partialFile.exists()) {
            partialFile.delete();
        }
    }

    private void safeUpdateProgress(FileTransfer transfer, int newProgress, UploadProgressCallback callback) {
        // 使用 CAS 或其他机制确保进度正确更新
        int currentProgress = transfer.getProgress();
        if (newProgress > currentProgress) {
            transfer.setProgress(newProgress);
            callback.onProgress(transfer, newProgress);
        }
    }
}