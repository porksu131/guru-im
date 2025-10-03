package com.guru.im.demo.gui.layout.content;

import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserConversation;
import com.guru.im.protocol.model.ConversationType;
import com.guru.im.protocol.model.ReadReceiptNotify;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RightPanel extends JPanel {
    private final Map<Long, ChatPanel> chatPanels = new HashMap<>();
    private final MainFrame mainFrame;
    private ChatPanel currentChatPanel;
    private JLabel topLabel;
    private JLabel placeholder;

    public RightPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(248, 249, 250));
        setBorder(BorderFactory.createEmptyBorder());

        placeholder = new JLabel("选择会话开始聊天", SwingConstants.CENTER);
        placeholder.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
        placeholder.setForeground(new Color(153, 153, 153));

        topLabel = new JLabel();
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        topLabel.setForeground(Color.BLACK);

        add(topLabel, BorderLayout.NORTH);
        add(placeholder, BorderLayout.CENTER);
    }


    public void showChatPanel(UserConversation userConversation) {
        if (userConversation == null) {
            return;
        }
        Long conversationId = userConversation.getConversationId();
        if (isCurrentChatPanelOpen(conversationId)) {
            return; // 已经打开会话窗口，退出
        }

        // 打开聊天窗口
        openChatPanel(userConversation);

        // 发送已读回执
        sendReadReceiptReq(userConversation);
    }

    public void sendReadReceiptReq(UserConversation userConversation) {
        if (userConversation.getUnreadCount() > 0) {
            if (userConversation.getConversationType() == ConversationType.PRIVATE_VALUE) {
                // 私聊发送已读回执成功后才更新未读数量
                this.mainFrame.getConversationListPanel().sendReadReceiptReq(userConversation);
            } else {
                // 群聊直接更新未读数量为零
                userConversation.setUnreadCount(0);
                userConversation.setLastReadSeq(userConversation.getLastMessageSeq());
                userConversation.setReadId(mainFrame.getCurrentUser().getUid());
                userConversation.setReadTime(System.currentTimeMillis());
                userConversation.setUpdateTime(System.currentTimeMillis());
                mainFrame.getDatabaseManager().saveOrUpdateUserConversation(userConversation);
                mainFrame.getDatabaseManager().updateMessagesToReadBySeq(userConversation.getConversationId(),
                        userConversation.getLastMessageSeq());
                mainFrame.getConversationListPanel().getConversationList().repaint();
            }

        }
    }

    public void openChatPanel(UserConversation userConversation) {
        Long conversationId = userConversation.getConversationId();
        this.placeholder.setVisible(false);
        //setBorder(BorderFactory.createTitledBorder("聊天窗口：" + userConversation.getShowName()));
        //topLabel.setText(userConversation.getShowName());
        if (currentChatPanel != null) {
            remove(currentChatPanel); // 先移除
        }
        if (!chatPanels.containsKey(conversationId)) {
            ChatPanel chatPanel = new ChatPanel(mainFrame, userConversation);
            currentChatPanel = chatPanel;
            chatPanels.put(conversationId, chatPanel);
        } else {
            currentChatPanel = chatPanels.get(conversationId);
        }
        add(currentChatPanel, BorderLayout.CENTER);// 再添加
        revalidate();
        repaint();
        currentChatPanel.scrollToBottom();
    }

    public void closeChatPanel() {
        if (currentChatPanel != null) {
            remove(currentChatPanel); // 先移除
            this.placeholder.setVisible(true);
            revalidate();
            repaint();
        }
    }


    public void addMessageToUI(Message message) {
        ChatPanel chatPanel = chatPanels.get(message.getConversationId());
        if (chatPanel != null) {
            chatPanel.addMessageToUI(message);
        }
    }

    public Map<Long, ChatPanel> getChatPanels() {
        return chatPanels;
    }

    public ChatPanel getCurrentChatPanel() {
        return currentChatPanel;
    }

    public boolean isCurrentChatPanelOpen(Long conversationId) {
        return currentChatPanel != null && currentChatPanel.conversationIdEquals(conversationId);
    }

    public void updateMessageRead(ReadReceiptNotify readReceiptNotify) {
        this.mainFrame.getDatabaseManager().updateMessagesToReadBySeq(readReceiptNotify.getConversationId(),
                readReceiptNotify.getLastReadSeq());
        // 更新界面
        ChatPanel chatPanel = this.getChatPanels().get(readReceiptNotify.getConversationId());
        if (chatPanel != null) {
            chatPanel.updateMessageRead(readReceiptNotify.getLastReadSeq());
        }
    }
}
