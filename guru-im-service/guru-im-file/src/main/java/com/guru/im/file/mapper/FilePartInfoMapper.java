package com.guru.im.file.mapper;

import com.guru.im.file.model.pojo.FilePartInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FilePartInfoMapper {
    int insert(FilePartInfo filePartInfo);

    List<FilePartInfo> selectByFileId(@Param("fileId") Long fileId);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") Long updateTime);

    int deleteByFileId(@Param("fileId") Long fileId);
}