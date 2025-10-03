package com.guru.im.user.mapper;

import com.guru.im.user.model.pojo.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendRelationMapper {
    List<FriendRelation> findFriendsByUid(@Param("uid") long uid);

    FriendRelation findByUidFriendId(@Param("uid") long uid, @Param("friendId") long friendId);

    int checkFriendExists(@Param("uid") long uid, @Param("friendId") long friendId);

    int save(FriendRelation friendRelation);

    int updateRelationStatus(FriendRelation friendRelation);

    int deleteByUidFriendId(@Param("uid") long uid, @Param("friendId") long friendId);

    List<FriendRelation> findByUidAndFriendIds(@Param("userId") Long userId,
                                               @Param("friendIds") List<Long> friendIds);
}
