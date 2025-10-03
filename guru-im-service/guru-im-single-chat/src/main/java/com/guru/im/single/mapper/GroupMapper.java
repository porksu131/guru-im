package com.guru.im.single.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMapper {
    List<Long> selectGroupMembers(@Param("groupId") Long groupId);
}