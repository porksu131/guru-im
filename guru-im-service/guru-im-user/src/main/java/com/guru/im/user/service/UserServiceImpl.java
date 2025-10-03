package com.guru.im.user.service;

import com.guru.im.user.mapper.UserMapper;
import com.guru.im.user.model.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> queryAllUsers() {
        return userMapper.queryAllUsers();
    }

    @Override
    public List<User> queryByUserName(String userName) {
        return userMapper.queryByUserName(userName);
    }

    @Override
    public List<User> queryByUserId(Long uid) {
        User user = userMapper.getByUid(uid);
        if (user != null) {
            return Collections.singletonList(user);
        }
        return null;
    }

    @Override
    public User getUserById(Long userId) {
        return userMapper.getByUid(userId);
    }
}
