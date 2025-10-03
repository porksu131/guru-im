package com.guru.im.demo.gui.conversation;

import com.guru.im.common.constant.MessageType;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.gui.component.RoundedTextField;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserConversation;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.protocol.model.*;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ConversationListPanel extends JPanel {
    private final MainFrame mainFrame;
    private final UserInfo currentUser;
    private DefaultListModel<UserConversation> listModel;
    private JList<UserConversation> conversationList;
    private JScrollPane scrollPane;
    private List<UserConversation> needFullSyncList = new ArrayList<>(); // 需要全量同步的会话列表

    public ConversationListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.currentUser = mainFrame.getCurrentUser();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initComponents();
        setupUI();
    }

    private void initComponents() {
        listModel = new DefaultListModel<>();
        conversationList = new JList<>(listModel);
        conversationList.setCellRenderer(new ConversationListRenderer());
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setLayoutOrientation(JList.VERTICAL);
        conversationList.setFixedCellHeight(58);
        conversationList.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        conversationList.setBackground(new Color(248, 249, 250));

        scrollPane = new JScrollPane(conversationList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        conversationList.addListSelectionListener((event) -> {
            UserConversation userConversation = conversationList.getSelectedValue();
            mainFrame.getChatPanelWrapper().showChatPanel(userConversation);
        });
    }

    private void setupUI() {
        // 创建搜索框
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        searchPanel.setBackground(new Color(248, 249, 250));

        RoundedTextField searchField = new RoundedTextField();
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.setPlaceholder("输入回车搜索");

        searchPanel.add(searchField, BorderLayout.CENTER);

        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadConversations() {
        // 获取会话列表，优先从本地获取
        List<UserConversation> userConversations = getUserConversations();

        // 加载会话列表到界面
        loadConversations(userConversations);

        // 检查是否有需要全量同步的会话
        checkNeedFullSync();
    }

    public void checkNeedFullSync() {
        Map<Long, Long> maxSequenceIdAllConversation = this.mainFrame.getDatabaseManager().getMaxSequenceIdAllConversation();
        if (maxSequenceIdAllConversation == null || maxSequenceIdAllConversation.isEmpty()) {
            return;
        }
        if (listModel.isEmpty()) {
            return;
        }
        for (int i = 0; i < listModel.getSize(); i++) {
            UserConversation f = listModel.getElementAt(i);
            Long maxSeq = maxSequenceIdAllConversation.get(f.getConversationId());
            if (maxSeq == null || maxSeq <= 0) {
                f.setLastMessageSeq(maxSeq);
                needFullSyncList.add(f);
            }
        }
    }

    public List<UserConversation> getUserConversations() {
        // 优先从本地加载会话信息
        List<UserConversation> userConversationsFromLocal = this.mainFrame.getDatabaseManager().getUserConversations(currentUser.getUid());
        List<UserConversation> userConversationsFromServer = getUserConversationsFromServer();
        if (CollectionUtils.isNotEmpty(userConversationsFromLocal)) {
            List<UserConversation> newUserConversations = findNewUserConversation(userConversationsFromLocal, userConversationsFromServer);
            if (!newUserConversations.isEmpty()) {
                this.mainFrame.getDatabaseManager().batchSaveUserConversations(newUserConversations);
                userConversationsFromLocal.addAll(newUserConversations);
            }

            return userConversationsFromLocal;
        }

        // 从服务器拉取最新会话信息
        return userConversationsFromServer;
    }

    private List<UserConversation> findNewUserConversation(List<UserConversation> localUserConversations,
                                                           List<UserConversation> remoteUserConversations) {
        List<UserConversation> newUserConversations = new ArrayList<>();
        for (UserConversation userConversation : remoteUserConversations) {
            boolean exist = localUserConversations.stream()
                    .anyMatch(obj -> Objects.equals(obj.getConversationId(),
                            userConversation.getConversationId()));
            if (!exist) {
                newUserConversations.add(userConversation);
            }
        }
        return newUserConversations;
    }


    public List<UserConversation> getUserConversationsFromServer() {
        // 从服务器拉取最新会话信息
        ResponseResult<List<UserConversation>> userConversationListResult =
                ApiService.getUserConversationList(currentUser.getUid(), currentUser.getAccessToken());
        if (ResponseResult.isSuccess(userConversationListResult)) {
            return userConversationListResult.getData();
        }
        return new ArrayList<>();
    }

    public void loadUserConversationsFromServer() {
        // 从服务器拉取最新会话信息
        List<UserConversation> userConversations = getUserConversationsFromServer();
        if (CollectionUtils.isEmpty(userConversations)) {
            return;
        }

        if (listModel.isEmpty()) {
            // 保存最新的会话信息到本地
            this.mainFrame.getDatabaseManager().batchSaveUserConversations(userConversations);
            listModel.addAll(userConversations);
        } else {
            List<UserConversation> newUserConversations = findNewConversation(userConversations);
            // 保存新增的会话信息到本地
            this.mainFrame.getDatabaseManager().batchSaveUserConversations(newUserConversations);
            if (!newUserConversations.isEmpty()) {
                listModel.addAll(newUserConversations);
            }

        }

        repaint();
    }

    public void loadUserConversationsFromServer(Long conversationId) {
        // 从服务器拉取最新会话信息
        List<UserConversation> userConversations = getUserConversationsFromServer();
        if (CollectionUtils.isEmpty(userConversations)) {
            return;
        }
        UserConversation userConversation = userConversations.stream()
                .filter(obj -> Objects.equals(obj.getConversationId(), conversationId))
                .findFirst().orElse(null);
        if (userConversation != null) {
            listModel.add(0, userConversation);
        }

        repaint();
    }

    private List<UserConversation> findNewConversation(List<UserConversation> userConversationList) {
        List<UserConversation> newUserConversations = new ArrayList<>();
        for (UserConversation userConversation : userConversationList) {
            if (!existConversation(userConversation.getConversationId())) {
                newUserConversations.add(userConversation);
            }
        }
        return newUserConversations;
    }

    public boolean existConversation(long conversationId) {
        for (int i = 0; i < listModel.getSize(); i++) {
            UserConversation f = listModel.getElementAt(i);
            if (f.getConversationId() == conversationId) {
                return true;
            }
        }
        return false;
    }


    public void loadConversations(List<UserConversation> conversations) {
        listModel.clear();
        for (UserConversation conversation : conversations) {
            listModel.addElement(conversation);
        }
        this.conversationList.repaint();
    }

    public void addConversation(UserConversation conversation) {
        listModel.addElement(conversation);
    }

    public void removeConversation(Long conversationId) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).getConversationId().equals(conversationId)) {
                listModel.remove(i);
                break;
            }
        }
    }

    public void clearUnReadCount(Long conversationId) {
        for (int i = 0; i < listModel.size(); i++) {
            UserConversation conversation = listModel.get(i);
            if (conversation.getConversationId().equals(conversationId)) {
                conversation.setUnreadCount(0);
                this.conversationList.repaint();
                break;
            }
        }
    }

    public void incrementUnreadCount(Long conversationId) {
        for (int i = 0; i < listModel.size(); i++) {
            UserConversation conversation = listModel.get(i);
            if (conversation.getConversationId().equals(conversationId)) {
                conversation.setUnreadCount(conversation.getUnreadCount() + 1);
                this.conversationList.repaint();
                break;
            }
        }
    }

    public void updateConversationWhenReceiveMessage(Message message, boolean isChatting) {
        for (int i = 0; i < listModel.size(); i++) {
            UserConversation userConversation = listModel.get(i);
            if (userConversation.getConversationId().equals(message.getConversationId())) {
                Message lastMaxSeqMessage = this.mainFrame.getDatabaseManager().getMaxSequenceMessageByConversation(message.getConversationId());
                if (lastMaxSeqMessage == null) {
                    lastMaxSeqMessage = message;
                }
                userConversation.setLastMessageContent(getConversationLastMessageContent(lastMaxSeqMessage));
                userConversation.setLastMessageTime(lastMaxSeqMessage.getTimestamp());
                userConversation.setLastMessageSeq(lastMaxSeqMessage.getServerSeq());
                userConversation.setLastMessageSender(lastMaxSeqMessage.getSenderId());
                userConversation.setUpdateTime(lastMaxSeqMessage.getTimestamp());
                int unreadCount = this.mainFrame.getDatabaseManager().getUnreadCountSentByMe(message.getConversationId());
                userConversation.setUnreadCount(unreadCount);
                if (isChatting) {
                    sendReadReceiptReq(userConversation);
                }
                this.mainFrame.getDatabaseManager().saveOrUpdateUserConversation(userConversation);
                this.conversationList.repaint();
                break;
            }
        }
    }

    public void updateConversationRead(ReadReceiptNotify readReceiptNotify) {
        for (int i = 0; i < listModel.size(); i++) {
            UserConversation userConversation = listModel.get(i);
            if (userConversation.getConversationId().equals(readReceiptNotify.getConversationId())) {
                userConversation.setReadTime(readReceiptNotify.getReadTime());
                userConversation.setReadId(readReceiptNotify.getReadId());
                userConversation.setLastReadSeq(readReceiptNotify.getLastReadSeq());
                userConversation.setUpdateTime(System.currentTimeMillis());
                this.mainFrame.getDatabaseManager().saveOrUpdateUserConversation(userConversation);
                break;
            }
        }
    }

    public void updateConversationWhenReceiveMessage(UserConversation updatedConversation) {
        for (int i = 0; i < listModel.size(); i++) {
            UserConversation conversation = listModel.get(i);
            if (conversation.getConversationId().equals(updatedConversation.getConversationId())) {
                listModel.set(i, updatedConversation);
                break;
            }
        }
    }

    public JList<UserConversation> getConversationList() {
        return conversationList;
    }

    public UserConversation getSelectedConversation() {
        return conversationList.getSelectedValue();
    }


    public void sendSyncRequest(OfflineSyncType offlineSyncType) {
        if (listModel.isEmpty()) {
            return;
        }
        OfflineSyncRequest offlineSyncRequest = buildOfflineSyncRequest(offlineSyncType);
        try {
            this.mainFrame.getImClientManager().user().sendSyncMessageRequest(offlineSyncRequest, null);
        } catch (Exception e) {
            //
        }
    }


    public OfflineSyncRequest buildOfflineSyncRequest(OfflineSyncType offlineSyncType) {
        return OfflineSyncRequest.newBuilder()
                .setUserId(currentUser.getUid())
                .setSyncId(0L)
                .setBatchSize(500)
                .setClientVersion("v1.0.0")
                .setSyncType(offlineSyncType)
                .setDeviceId(this.mainFrame.getDeviceInfo().getDeviceId())
                .setNetworkType("wired")
                .putAllClientCursorMap(buildConversationCursorMap(offlineSyncType))
                .build();
    }

    private Map<Long, Long> buildConversationCursorMap(OfflineSyncType offlineSyncType) {
        Map<Long, Long> conversationCursorMap = new HashMap<>();
        if (offlineSyncType == OfflineSyncType.SYNC_TYPE_INCREMENTAL) {
            for (int i = 0; i < listModel.size(); i++) {
                UserConversation conversation = listModel.get(i);
                if (conversation.getLastMessageSeq() == null) {
                    conversationCursorMap.put(conversation.getConversationId(), 0L);
                } else {
                    conversationCursorMap.put(conversation.getConversationId(), conversation.getLastMessageSeq());
                }

            }
            return conversationCursorMap;
        } else if (offlineSyncType == OfflineSyncType.SYNC_TYPE_FULL) {
            for (UserConversation conversation : needFullSyncList) {
                if (conversation.getLastMessageSeq() == null) {
                    conversationCursorMap.put(conversation.getConversationId(), 0L);
                } else {
                    conversationCursorMap.put(conversation.getConversationId(), conversation.getLastMessageSeq());
                }
            }
            return conversationCursorMap;
        }
        return new HashMap<>();
    }

    public void sendReadReceiptReq(UserConversation userConversation) {
        ReadReceiptReq readReceiptReq = buildReadReceiptReq(userConversation);
        try {
            this.mainFrame.getImClientManager().chat().sendReadReceiptReq(readReceiptReq, new MessageSendCallback() {
                @Override
                public void onSuccess() {
                    // 更新聊天会话的已读状态
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

                @Override
                public void onFailure(String errorMessage) {
                    System.err.println("Failed to sendReadReceiptReq: " + errorMessage);
                }
            });
        } catch (Exception e) {
            //
        }

    }

    public void updateUserConversationRead(UserConversation userConversation) {
        // 更新聊天会话的已读状态
        userConversation.setUnreadCount(0);
        userConversation.setLastReadSeq(userConversation.getLastMessageSeq());
        userConversation.setReadId(mainFrame.getCurrentUser().getUid());
        userConversation.setReadTime(System.currentTimeMillis());
        userConversation.setUpdateTime(System.currentTimeMillis());
        mainFrame.getDatabaseManager().saveOrUpdateUserConversation(userConversation);
    }

    public void updateConversationLastMessage(Message message, UserConversation conversation) {
        conversation.setLastMessageContent(getConversationLastMessageContent(message));
        conversation.setLastMessageTime(message.getTimestamp());
        conversation.setLastMessageSeq(message.getServerSeq());
        conversation.setLastMessageSender(message.getSenderId());
        conversation.setLastSenderNickname(message.getSenderName());
        conversation.setUpdateTime(message.getTimestamp());
        mainFrame.getDatabaseManager().saveOrUpdateUserConversation(conversation);
        this.conversationList.repaint();
    }

    public ReadReceiptReq buildReadReceiptReq(UserConversation userConversation) {
        return ReadReceiptReq.newBuilder()
                .setConversationId(userConversation.getConversationId())
                .setReadId(userConversation.getUserId())
                .setLastReadSeq(userConversation.getLastMessageSeq())
                .setConversationType(ConversationType.forNumber(userConversation.getConversationType()))
                .build();
    }

    private String getConversationLastMessageContent(Message message) {
        switch (message.getMessageType()) {
            case MessageType.TEXT:
                return message.getMessageContent();

            case MessageType.IMAGE:
                return "[图片]";

            case MessageType.FILE:
                return "[文件] " + message.getMediaInfo().getFileName();

            case MessageType.VOICE:
                return "[语音]";

            case MessageType.VIDEO:
                return "[视频]";

            case MessageType.LOCATION:
                return "[位置]";

            default:
                return "[新消息]";
        }
    }

    public boolean isNeedFullSync() {
        return !needFullSyncList.isEmpty();
    }

    public List<UserConversation> getNeedFullSyncList() {
        return needFullSyncList;
    }
}