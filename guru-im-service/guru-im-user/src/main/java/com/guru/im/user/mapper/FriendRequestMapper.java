package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    List<FriendRequest> findByRequester(@Param("requesterId") long requesterId);

    List<FriendRequest> findByResponder(@Param("responderId") long responderId);

    int checkRequestExists(@Param("requesterId") long requesterId, @Param("responderId") long responderId);

    int save(FriendRequest friendRequest);

    int updateRequestStatus(FriendRequest friendRequest);

    int batchUpdateStatus(List<Long> ids, int notifyStatus, long updateTime);

    FriendRequest findByRequesterResponder(@Param("requesterId") long requesterId, @Param("responderId") long responderId);

    FriendRequest findById(@Param("id") long id);
}
