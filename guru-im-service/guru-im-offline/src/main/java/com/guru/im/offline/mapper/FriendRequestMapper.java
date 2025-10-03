package com.guru.im.offline.mapper;

import com.guru.im.offline.model.pojo.FriendRequest;
import com.guru.im.offline.model.pojo.OfflineMessageDelivery;
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

    List<OfflineMessageDelivery> selectUndeliveredByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);
    int updateDeliveryStatusBatch(@Param("ids") List<Long> ids,
                                  @Param("status") Integer status,
                                  @Param("errorReason") String errorReason,
                                  @Param("updateTime") Long updateTime);
}
