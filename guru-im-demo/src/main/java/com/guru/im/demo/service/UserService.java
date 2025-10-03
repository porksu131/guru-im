package com.guru.im.demo.service;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.model.UserInfo;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    public static final Map<Long, UserInfo> userServiceMap = new HashMap<>();
    private static UserInfo currentUser = null;

    public static void setCurrentUser(UserInfo currentUser) {
        UserService.currentUser = currentUser;
    }

    public static UserInfo getUserInfo(Long userId) {
        UserInfo userInfo = userServiceMap.get(userId);
        if (userInfo == null) {
            ResponseResult<UserInfo> responseResult = ApiService.getByUserId(userId, currentUser.getAccessToken());
            if (ResponseResult.isSuccess(responseResult) && responseResult.getData() != null) {
                userInfo = responseResult.getData();
                userServiceMap.put(userId, userInfo);
            }
        }
        return userInfo;
    }

    public static String getUserName(Long userId) {
        UserInfo userInfo = getUserInfo(userId);
        return userInfo != null ? userInfo.getUserName() : "";
    }
}
