package com.guru.im.user.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.user.model.pojo.FriendRequest;
import com.guru.im.user.model.vo.FriendRelationVO;
import com.guru.im.user.model.vo.FriendRequestVO;

import java.util.List;

public interface FriendService {
    ResponseResult<List<FriendRelationVO>> getFriendsByUserId(long uid);

    ResponseResult<Void> addFriend(long uid, long friendId);

    ResponseResult<Void> handleFriendRequest(FriendRequestVO request);

    ResponseResult<FriendRequest> sendFriendRequest(FriendRequestVO request);
}