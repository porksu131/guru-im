package com.guru.im.auth.service;

import com.guru.im.auth.mapper.ConversationMapper;
import com.guru.im.auth.mapper.UserConversationMapper;
import com.guru.im.auth.mapper.UserMapper;
import com.guru.im.auth.model.CustomUserDetails;
import com.guru.im.auth.model.pojo.Conversation;
import com.guru.im.auth.model.pojo.FriendRelation;
import com.guru.im.auth.model.pojo.UserConversation;
import com.guru.im.auth.model.pojo.UserInfo;
import com.guru.im.common.constant.Constants;
import com.guru.im.common.exception.ServiceException;
import com.guru.im.common.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private UserConversationMapper userConversationMapper;

    // security在做密码校验时会从这里查询用户信息
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo user = userMapper.getByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 应从数据库查询角色，这里简化为固定角色
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("admin"));

        CustomUserDetails customUserDetails = new CustomUserDetails();
        customUserDetails.setUsername(username);
        customUserDetails.setPassword(user.getPassword());
        customUserDetails.setUserId(user.getUid());
        customUserDetails.setAuthorities(authorities);
        return customUserDetails;
    }

    public UserInfo loadUserByUid(Long uid) {
        return userMapper.getByUid(uid);
    }

    public UserInfo saveUser(String userName, String password, String phone, boolean isCreateDefaultRelation) {
        if (userMapper.getByUserName(userName) != null) {
            throw new ServiceException(Constants.FAIL, "用户名已存在");
        }
        Long userId = snowflakeIdGenerator.nextId();
        Long conversationId = snowflakeIdGenerator.nextId();

        UserInfo user = new UserInfo();
        user.setUid(userId);
        user.setUserName(userName);
        user.setPhone(phone);
        user.setEnabledFlag(true);
        user.setPassword(passwordEncoder.encode(password)); // 使用BCrypt加密密码
        userMapper.save(user);

        if (isCreateDefaultRelation) {
            createFriendRelation(user);
            createConversation(conversationId, user);
            createUserConversation(conversationId, user);
        }

        return user;
    }

    private void createFriendRelation(UserInfo user) {
        FriendRelation relation = new FriendRelation(); // 将自己添加到好友关系
        relation.setId(snowflakeIdGenerator.nextId());
        relation.setUid(user.getUid());
        relation.setFriendId(user.getUid());
        relation.setUserName(user.getUserName());
        relation.setFriendName("我");
        relation.setRelationStatus(1);
        relation.setCreateTime(System.currentTimeMillis());
        relation.setUpdateTime(System.currentTimeMillis());

        userMapper.saveFriendRelation(relation);
    }

    private void createConversation(Long conversationId, UserInfo userInfo) {
        long now = System.currentTimeMillis();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setConversationType(1); // 1:私聊
        conversation.setConversationKey(userInfo.getUid() + "_" + userInfo.getUid());
        conversation.setConversationName("我");
        conversation.setStatus(1);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        conversation.setVersion(1L);
        conversationMapper.insert(conversation);
    }

    private void createUserConversation(Long conversationId, UserInfo userInfo) {
        long now = System.currentTimeMillis();
        UserConversation userConversation = new UserConversation();
        userConversation.setId(snowflakeIdGenerator.nextId());
        userConversation.setUserId(userInfo.getUid());
        userConversation.setConversationId(conversationId);
        userConversation.setConversationType(1);
        userConversation.setShowNickname("我");
        userConversation.setCreateTime(now);
        userConversation.setUpdateTime(now);
        userConversation.setStatus(1);
        userConversationMapper.insert(userConversation);
    }
}