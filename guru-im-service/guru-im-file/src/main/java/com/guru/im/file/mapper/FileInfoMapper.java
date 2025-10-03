package com.guru.im.file.mapper;

import com.guru.im.file.model.pojo.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileInfoMapper {
    int insert(FileInfo fileInfo);

    FileInfo selectByMd5(@Param("md5") String md5);

    FileInfo selectById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") Long updateTime);
}