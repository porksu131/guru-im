package com.guru.im.signal.mapper;

import com.guru.im.signal.model.pojo.MediaRoomMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MediaRoomMapper {
    
    int insert(MediaRoomMapping mediaRoomMapping);
    
    MediaRoomMapping selectBySessionId(@Param("sessionId") Long sessionId);
    
    MediaRoomMapping selectByRoomId(@Param("roomId") String roomId);
    
    int updateRoomStatus(@Param("roomId") String roomId,
                        @Param("roomStatus") Integer roomStatus,
                        @Param("updateTime") Long updateTime);
    
    int updateActivePeers(@Param("roomId") String roomId,
                         @Param("activePeers") Integer activePeers,
                         @Param("updateTime") Long updateTime);
    
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}