package com.guru.im.user.controller;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.user.model.pojo.Conversation;
import com.guru.im.user.model.pojo.UserConversation;
import com.guru.im.user.model.vo.UserConversationVO;
import com.guru.im.user.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/conversation")
public class ConversationController {
    @Autowired
    private ConversationService conversationService;

    // 好友列表
    @PostMapping("/list")
    public ResponseResult<List<UserConversationVO>> list(@RequestBody Map<String, Long> reqMap) {
        Long uid = reqMap.get("uid");
        return ResponseResult.ok(conversationService.getUserConversationsOld(uid));
    }

    @PostMapping("/updateSetting")
    public ResponseResult<Boolean> updateConversationSetting(@RequestParam Long userId,
                                                             @RequestParam Long conversationId,
                                                             @RequestParam(required = false) Boolean isTop,
                                                             @RequestParam(required = false) Boolean isMute) {
        return conversationService.updateConversationSetting(userId, conversationId, isTop, isMute);
    }

    @PostMapping("/delete")
    public ResponseResult<Boolean> deleteUserConversation(@RequestParam Long userId,
                                                          @RequestParam Long conversationId) {
        return conversationService.deleteUserConversation(userId, conversationId);
    }

    @GetMapping("/detail")
    public ResponseResult<UserConversation> getConversationDetail(
            @RequestParam Long userId,
            @RequestParam Long conversationId) {
        return conversationService.getUserConversationDetail(userId, conversationId);
    }

    @GetMapping("/group/{groupId}")
    public ResponseResult<Conversation> getGroupConversation(@PathVariable Long groupId) {
        String conversationKey = "group_" + groupId;
        Conversation conversation = conversationService.selectConversationByKey(conversationKey);
        if (conversation == null) {
            return ResponseResult.fail("群聊会话不存在");
        }
        return ResponseResult.ok(conversation);
    }
}
