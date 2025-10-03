package com.guru.im.auth.mapper;

import com.guru.im.auth.model.pojo.FriendRelation;
import com.guru.im.auth.model.pojo.UserInfo;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    UserInfo getByUserName(@Param("userName") String userName);

    UserInfo getByUid(@Param("uid") long uid);

    int save(UserInfo user);

    int saveFriendRelation(FriendRelation friendRelation);
}
