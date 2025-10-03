package com.guru.im.user.service;

import com.guru.im.user.model.pojo.User;

import java.util.List;

public interface UserService {
    List<User> queryAllUsers();

    List<User> queryByUserName(String userName);

    List<User> queryByUserId(Long uid);

    User getUserById(Long userId);
}
