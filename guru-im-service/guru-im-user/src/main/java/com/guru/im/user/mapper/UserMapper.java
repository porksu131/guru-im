package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    User getByUserName(@Param("userName") String userName);
    User getByUid(@Param("uid") long uid);
    int save(User user);
    List<User> queryAllUsers();

    List<User> queryByUserName(@Param("userName") String userName);
}
