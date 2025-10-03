package com.guru.im.user.controller;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.user.model.pojo.FriendRequest;
import com.guru.im.user.service.FriendService;
import com.guru.im.user.model.vo.FriendRelationVO;
import com.guru.im.user.model.vo.FriendRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friend")
public class FriendController {
    @Autowired
    private FriendService friendService;

    // 好友列表
    @PostMapping("/list")
    public ResponseResult<List<FriendRelationVO>> list(@RequestBody Map<String, Long> reqMap) {
        Long uid = reqMap.get("uid");
        return friendService.getFriendsByUserId(uid);
    }

    // 添加好友（旧）
    @PostMapping("/add")
    public ResponseResult<Void> add(@RequestBody Map<String, Long> reqMap) {
        Long uid = reqMap.get("uid");
        Long friendId = reqMap.get("friendId");
        return friendService.addFriend(uid, friendId);
    }

    // 发送好友请求
    @PostMapping("/request")
    public ResponseResult<FriendRequest> sendFriendRequest(@RequestBody FriendRequestVO request) {
        return friendService.sendFriendRequest(request);
    }

    // 处理好友请求
    @PostMapping("/response")
    public ResponseResult<Void> handleFriendRequest(@RequestBody FriendRequestVO request) {
        return friendService.handleFriendRequest(request);
    }
}
