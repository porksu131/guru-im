package com.guru.im.file.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import com.guru.im.file.config.MinioProperties;
import com.guru.im.file.mapper.FileInfoMapper;
import com.guru.im.file.mapper.FilePartInfoMapper;
import com.guru.im.file.model.pojo.FileInfo;
import com.guru.im.file.model.pojo.FilePartInfo;
import com.guru.im.file.model.vo.UploadInitRequest;
import com.guru.im.file.model.vo.UploadInitResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    private final FileInfoMapper fileInfoMapper;
    private final FilePartInfoMapper filePartInfoMapper;
    private final MinioService minioService;
    private final MinioProperties minioProperties;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final String bucketName;
    private final Long defaultPartSize;

    public FileServiceImpl(FileInfoMapper fileInfoMapper,
                           FilePartInfoMapper filePartInfoMapper,
                           MinioService minioService, MinioProperties minioProperties,
                           SnowflakeIdGenerator snowflakeIdGenerator) {
        this.fileInfoMapper = fileInfoMapper;
        this.filePartInfoMapper = filePartInfoMapper;
        this.minioService = minioService;
        this.minioProperties = minioProperties;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.bucketName = minioProperties.getChatBucket();
        this.defaultPartSize = minioProperties.getDefaultPartSize();
    }

    @Override
    public FileInfo checkFileExist(String md5) {
        return fileInfoMapper.selectByMd5(md5);
    }

    @Override
    @Transactional
    public ResponseResult<UploadInitResponse> initFileUpload(UploadInitRequest initRequest) {
        // 1. 秒传检查
        FileInfo existFile = fileInfoMapper.selectByMd5(initRequest.getMd5());
        if (existFile != null) {
            // 全部上传完成
            if (existFile.getStatus() == 1) {
                // 重新生成新的下载url，避免过期
                existFile.setDownloadUrl(minioService.getPreSignedGetObjectUrl(bucketName, existFile.getPath()));
                return ResponseResult.ok(new UploadInitResponse(existFile, null));
            }
            // 部分上传完成，返回分片信息
            List<FilePartInfo> filePartInfos = filePartInfoMapper.selectByFileId(existFile.getId());
            // 重新生成上传url，避免过期
            getAndSetPreSignedUrl(filePartInfos, existFile);
            return ResponseResult.ok(new UploadInitResponse(existFile, filePartInfos));
        }

        // 2. 保存文件元信息
        FileInfo fileInfo = saveFileInfo(initRequest);

        // 3. 初始化分片信息
        List<FilePartInfo> partInfos = initPartUpload(fileInfo.getId(), initRequest);

        // 4. 生成分片预签名url
        getAndSetPreSignedUrl(partInfos, fileInfo);

        // 5. 保存分片信息
        savePartUpload(partInfos, fileInfo);

        return ResponseResult.ok(new UploadInitResponse(fileInfo, partInfos));
    }

    @Override
    public String getUploadUrl(Long fileId, Integer partNumber) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null || fileInfo.getStatus() != 0) {
            return null;
        }

        String objectName = fileInfo.getPath() + "/part_" + partNumber;
        return minioService.getPreSignedPutObjectUrl(bucketName, objectName);
    }

    @Override
    @Transactional
    public boolean completePartUpload(Long fileId, Integer partNumber, String partMd5) {
        long now = System.currentTimeMillis();
        FilePartInfo partInfo = filePartInfoMapper.selectByFileId(fileId)
                .stream()
                .filter(p -> Objects.equals(p.getPartNumber(), partNumber))
                .findFirst()
                .orElse(null);

        if (partInfo == null) {
            return false;
        }

        partInfo.setPartMd5(partMd5);
        partInfo.setStatus(1); // 已上传
        partInfo.setUpdateTime(now);

        return filePartInfoMapper.updateStatus(partInfo.getId(), 1, partInfo.getUpdateTime()) > 0;
    }

    @Override
    @Transactional
    public ResponseResult<FileInfo> completeFileUpload(Long fileId) {
        // 检查文件是否存在
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            return ResponseResult.fail("no file found");
        }

        // 检查所有分片是否都已上传
        List<FilePartInfo> partInfos = filePartInfoMapper.selectByFileId(fileId);
        boolean allPartsUploaded = partInfos.stream().allMatch(p -> p.getStatus() == 1);

        if (!allPartsUploaded) {
            return ResponseResult.fail("parts not all uploaded");
        }

        boolean success = doCompleteFileUpload(fileInfo, partInfos);
        if (!success) {
            return ResponseResult.fail("complete file upload failed");
        }
        fileInfo.setDownloadUrl(minioService.getPreSignedGetObjectUrl(bucketName, fileInfo.getPath()));
        return ResponseResult.ok(fileInfo);
    }

    @Transactional
    public boolean doCompleteFileUpload(FileInfo fileInfo, List<FilePartInfo> partInfos) {
        long now = System.currentTimeMillis();
        long fileId = fileInfo.getId();


        // 合并分片
        String sourcePath = fileInfo.getPath();
        String targetPath = fileInfo.getPath();

        // 目前设计：只有一个分片的话时不需要合并分片的
        if (partInfos.size() == 1) {
            return fileInfoMapper.updateStatus(fileId, 1, now) > 0; // 上传完成
        }

        try {
            // 合并分片
            List<String> partNames = new ArrayList<>();
            for (FilePartInfo partInfo : partInfos) {
                partNames.add(sourcePath + "/part_" + partInfo.getPartNumber());
            }

            minioService.composeObject(bucketName, targetPath, partNames);

            // 删除分片
            for (String partName : partNames) {
                minioService.removeObject(bucketName, partName);
            }

            // 更新文件状态
            return fileInfoMapper.updateStatus(fileId, 1, now) > 0; // 上传完成
        } catch (Exception e) {
            throw new RuntimeException("合并文件失败", e);
        }
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null || fileInfo.getStatus() != 1) {
            return null;
        }
        return minioService.getPreSignedGetObjectUrl(bucketName, fileInfo.getPath());
    }

    @Override
    public FileInfo getFileInfo(Long fileId) {
        return fileInfoMapper.selectById(fileId);
    }


    private List<FilePartInfo> initPartUpload(Long fileId, UploadInitRequest initRequest) {
        long now = System.currentTimeMillis();

        // 计算分片大小 默认10M
        long partSize = initRequest.getPartSize() != null ? initRequest.getPartSize() : this.defaultPartSize;
        int partCount = (int) Math.ceil((double) initRequest.getSize() / partSize);

        List<FilePartInfo> partInfos = new ArrayList<>();

        for (int i = 1; i <= partCount; i++) {
            FilePartInfo partInfo = new FilePartInfo();
            partInfo.setId(snowflakeIdGenerator.nextId());
            partInfo.setFileId(fileId);
            partInfo.setPartNumber(i);
            partInfo.setPartSize(partSize);
            partInfo.setPartMd5(null);
            partInfo.setStatus(0); // 未上传
            partInfo.setCreateTime(now);
            partInfo.setUpdateTime(now);
            partInfos.add(partInfo);
        }

        return partInfos;
    }

    private void getAndSetPreSignedUrl(List<FilePartInfo> partInfos, FileInfo fileInfo) {
        if (partInfos.size() == 1) {
            // 单个
            String preSignedUrl = minioService.getPreSignedPutObjectUrl(bucketName, fileInfo.getPath());
            partInfos.get(0).setPreSignedUrl(preSignedUrl);
        } else {
            // 多个
            for (FilePartInfo partInfo : partInfos) {
                if (partInfo.getStatus() == 1) {
                    continue;
                }
                String preSignedUrl = minioService.getPreSignedPutObjectUrl(bucketName, fileInfo.getPath() + "/part_" + partInfo.getPartNumber());
                partInfo.setPreSignedUrl(preSignedUrl);
            }
        }
    }

    private void setPreSignedUrl(List<FilePartInfo> partInfos, FileInfo fileInfo) {
        if (partInfos.size() == 1) {
            // 单个
            String preSignedUrl = minioService.getPreSignedPutObjectUrl(bucketName, fileInfo.getPath());
            partInfos.get(0).setPreSignedUrl(preSignedUrl);
        } else {
            // 多个
            int index = 1;
            for (FilePartInfo partInfo : partInfos) {
                String preSignedUrl = minioService.getPreSignedPutObjectUrl(bucketName, fileInfo.getPath() + "/part_" + partInfo.getPartNumber());
                partInfo.setPreSignedUrl(preSignedUrl);
                index++;
            }
        }
    }

    private void savePartUpload(List<FilePartInfo> partInfos, FileInfo fileInfo) {
        // 先删除旧的分片信息(如果有)
        filePartInfoMapper.deleteByFileId(fileInfo.getId());
        for (FilePartInfo partInfo : partInfos) {
            filePartInfoMapper.insert(partInfo);
        }
    }

    private FileInfo saveFileInfo(UploadInitRequest initRequest) {
        long now = System.currentTimeMillis();

        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(snowflakeIdGenerator.nextId());
        fileInfo.setOriginalName(initRequest.getOriginalName());
        fileInfo.setStorageName(UUID.randomUUID().toString().replace("-", ""));
        fileInfo.setSize(initRequest.getSize());
        fileInfo.setMd5(initRequest.getMd5());
        fileInfo.setContentType(initRequest.getContentType());
        fileInfo.setPath(renameFilePath(initRequest)); // 文件重命名
        fileInfo.setUserId(initRequest.getUserId());
        fileInfo.setStatus(0); // 上传中
        fileInfo.setCreateTime(now);
        fileInfo.setUpdateTime(now);

        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    private String renameFilePath(UploadInitRequest initRequest) {
        String originFileName = initRequest.getOriginalName();
        String suffix = FileUtil.extName(originFileName);
        String fileName = FileUtil.mainName(originFileName);
        // 对文件重命名，并以年月日文件夹格式存储
        String date = DateUtil.format(LocalDateTime.now(), "yyyyMMdd");
        return "/" + initRequest.getUserId() + "/" + date + "/" + fileName + "_" + initRequest.getMd5() + "." + suffix;
    }
}