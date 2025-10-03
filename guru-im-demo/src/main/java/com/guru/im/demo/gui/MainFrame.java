package com.guru.im.demo.gui;

import com.guru.im.common.constant.NetworkType;
import com.guru.im.common.constant.OnlineStatus;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.core.common.listener.MessageSendCallback;
import com.guru.im.demo.convert.MessageConverter;
import com.guru.im.demo.gui.conversation.ConversationListPanel;
import com.guru.im.demo.gui.friend.FriendListPanel;
import com.guru.im.demo.gui.group.GroupListPanel;
import com.guru.im.demo.gui.layout.content.ContentPanel;
import com.guru.im.demo.gui.layout.content.RightPanel;
import com.guru.im.demo.gui.layout.header.HeaderPanel;
import com.guru.im.demo.gui.menu.PopupMenuItemInfo;
import com.guru.im.demo.gui.menu.PopupMenuUtil;
import com.guru.im.demo.listener.*;
import com.guru.im.demo.model.*;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import com.guru.im.demo.service.TokenManager;
import com.guru.im.demo.service.UserService;
import com.guru.im.demo.sqlite.DatabaseManager;
import com.guru.im.protocol.model.*;
import com.guru.im.sdk.IMClientManager;
import com.guru.im.sdk.config.IMConfig;
import com.guru.im.sdk.constant.ConnectionConstant;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFrame extends JFrame {
    private final URL imageURL = MainFrame.class.getClassLoader().getResource("image/chat-logo.png");
    private final String clientVersion = "v1.0.0";
    private final String networkType = NetworkType.LAN;
    private final UserInfo currentUser;
    private final DeviceInfo deviceInfo;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final DatabaseManager databaseManager;
    private final IMClientManager imClientManager;
    private final TokenManager tokenManager;
    private boolean isLogout = false;

    private PopupMenuUtil popupMenuUtil;
    private ContentPanel contentPanel;
    private HeaderPanel headerPanel;

    public MainFrame(UserInfo user) throws Exception {
        this.currentUser = user;
        this.tokenManager = new TokenManager(this);
        ApiService.setTokenManager(tokenManager);
        UserService.setCurrentUser(user);
        this.databaseManager = new DatabaseManager(currentUser);
        this.deviceInfo = initDeviceInfo();
        this.imClientManager = initIMClientManager();
        initPopupMenuUtil();
        setupUI();
        start();
    }

    public void start() {
        imClientManager.connect();
    }

    private IMClientManager initIMClientManager() {
        IMConfig imConfig = IMConfig.newBuilder()
                .serverAddress(ApiService.GATEWAY_TCP_SERVER)
                .sendTimeout(15000) // 15秒
                .maxReconnectAttempts(10) // 重连最大尝试3次
                .maxReconnectDelay(10 * 60000) // 重连最大延迟时间 10分钟
                .autoReconnect(true) // 开启自动重连
                .initialReconnectDelay(3000) // 断开连接3秒后开始重连，并按照3秒的指数退避重连
                .heartbeatInterval(1000) // 心跳间隔 10秒
                .appKey(null)
                .build();
        com.guru.im.sdk.model.UserInfo imUserInfo = new com.guru.im.sdk.model.UserInfo();
        imUserInfo.setUserId(this.currentUser.getUid());
        imUserInfo.setUserName(this.currentUser.getUserName());
        imUserInfo.setAccessToken(this.currentUser.getAccessToken());
        imUserInfo.setRefreshToken(this.currentUser.getRefreshToken());

        IMClientManager imClientManager = new IMClientManager(imConfig, imUserInfo, deviceInfo);
        imClientManager.addEventListener(new SyncMessageListener(this));
        imClientManager.addEventListener(new ChatMessageListener(this));
        imClientManager.addEventListener(new PresenceNotifyListener(this));
        imClientManager.addEventListener(new ConnectChangeListener(this));
        imClientManager.addEventListener(new FriendRequestNotifyListener(this));
        imClientManager.addEventListener(new ReadReceiptListener(this));
        imClientManager.addEventListener(new OfflineEventListener(this));
        imClientManager.addEventListener(new GroupInviteNotifyListener(this));
        imClientManager.addEventListener(new OfflineDeviceListener(this));
        return imClientManager;
    }

    private DeviceInfo initDeviceInfo() {
        return DeviceInfo.newBuilder()
                .setDeviceId("window10")
                .setDeviceName("window10")
                .setClientVersion(getClientVersion())
                .setPlatform(DeviceInfo.PlatformType.WINDOWS)
                .build();
    }

    // 初始化右键菜单
    private void initPopupMenuUtil() {
        List<PopupMenuItemInfo> popupMenuItemInfoList = new ArrayList<>();
        PopupMenuItemInfo popupMenuItemInfo1 = new PopupMenuItemInfo();
        popupMenuItemInfo1.setText("刷新好友列表");
        popupMenuItemInfoList.add(popupMenuItemInfo1);
        PopupMenuItemInfo popupMenuItemInfo2 = new PopupMenuItemInfo();
        popupMenuItemInfo2.setText("拉取最新消息");
        popupMenuItemInfoList.add(popupMenuItemInfo2);
        this.popupMenuUtil = new PopupMenuUtil(popupMenuItemInfoList);
    }

    // 主界面
    private void setupUI() {
        setTitle("  聊天客户端");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon(imageURL).getImage());

        // 头部面板
        this.headerPanel = new HeaderPanel(this);

        // 主内容区
        this.contentPanel = new ContentPanel(this);


        // 主面板使用GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 242, 245));
        mainPanel.setBorder(BorderFactory.createEmptyBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 2, 0, 2);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(headerPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        mainPanel.add(contentPanel, gbc);

        add(mainPanel);
    }

    public void loadData() {
        executorService.execute(() -> {
            // 本地加载会话列表
            this.getConversationListPanel().loadConversations();
            // 本地加载好友列表
            this.getFriendListPanel().loadFriendList();
            // 本地加载群聊列表
            this.getGroupListPanel().loadGroupList();
            // 本地加载系统通知
            this.updateNotificationUnreadCount();

            // 如需全量则发起全量消息同步
            if (this.getConversationListPanel().isNeedFullSync()) {
                this.getConversationListPanel().sendSyncRequest(OfflineSyncType.SYNC_TYPE_FULL);
            }
            // 发起增量消息同步
            this.getConversationListPanel().sendSyncRequest(OfflineSyncType.SYNC_TYPE_INCREMENTAL);
            // 发起增量事件同步
            this.sendSyncEventRequest();

        });

    }

    // 发送增量事件同步请求
    public void sendSyncEventRequest() {
        SyncEventRequest syncEventRequest = SyncEventRequest.newBuilder()
                .setUserId(this.currentUser.getUid())
                .setDeviceId(this.deviceInfo.getDeviceId())
                .setSyncSize(500)
                .setNetworkType(getNetworkType())
                .setClientVersion(getClientVersion())
                .setSyncType(OfflineSyncType.SYNC_TYPE_INCREMENTAL)
                .setLastSequence(this.getDatabaseManager().getLastSyncSeq(currentUser.getUid()))
                .build();
        try {
            this.imClientManager.user().sendSyncEventsRequest(syncEventRequest, new MessageSendCallback() {
            });
        } catch (Exception e) {
            //
        }
    }

    // 更新通知未读数量
    public void updateNotificationUnreadCount() {
        int notificationUnreadCount = databaseManager.getNotificationUnreadCount();
        this.headerPanel.getFunctionPanel().updateNotificationCount(notificationUnreadCount);
    }


    public void logout() {
        ResponseResult<Void> logout = ApiService.logout(currentUser.getAccessToken());
        if (ResponseResult.isSuccess(logout)) {
            isLogout = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(MainFrame.this,
                    "登出异常：" + logout.getMsg(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void dispose() {
        executorService.execute(() -> {
            try {
                this.imClientManager.user().logout();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.imClientManager.disconnect();
        });
        new LoginFrame().setVisible(true);
        super.dispose();
    }


    // 收到用户在线状态变更消息
    public void onReceivePresenceNotify(PresenceNotify presenceNotify) {
        SwingUtilities.invokeLater(() -> {
            this.getFriendListPanel().receiveFriendNotify(presenceNotify);
        });
    }

    // 收到好友请求通知
    public void onReceiveFriendRequestNotify(FriendRequestNotify friendRequestNotify) {
        SwingUtilities.invokeLater(() -> {
            FriendRequest friendRequest = MessageConverter.convertToClientFriendRequest(friendRequestNotify);
            databaseManager.saveFriendRequest(friendRequest);

            Notification notification = MessageConverter.convertToClientFriendNotify(currentUser, friendRequestNotify);
            databaseManager.saveNotification(notification);

            if (!(friendRequestNotify.getRequesterId() == currentUser.getUid()
                    && friendRequestNotify.getRequestStatus() == RequestStatus.PENDING)) {
                // 除了是自己发送且待对方处理的通知，其他都要显示通知提醒
                updateNotificationUnreadCount();
            }

            if (friendRequest.getRequestStatus() == FriendRequest.RequestStatus.ACCEPTED.getValue()) {
                // 重新加载好友列表
                this.getFriendListPanel().loadFriendList();
                // 重新加载会话列表
                this.getConversationListPanel().loadUserConversationsFromServer();
            }
        });
    }

    // 收到群邀请通知
    public void onReceiveGroupInviteNotify(GroupInviteNotify groupInviteNotify) {
        SwingUtilities.invokeLater(() -> {
            GroupInvite groupInvite = MessageConverter.convertToClientGroupInvite(groupInviteNotify);
            databaseManager.saveGroupInvite(groupInvite);

            Notification notification = MessageConverter.convertToClientGroupInviteNotify(groupInviteNotify);
            databaseManager.saveNotification(notification);

            if (groupInviteNotify.getInviterId() != currentUser.getUid()) {
                // 除了自己发起的，其他都要显示通知提醒
                updateNotificationUnreadCount();
            }

            if (groupInvite.getRequestStatus() == RequestStatus.ACCEPTED) {
                // 重新加载群组列表
                this.getGroupListPanel().loadGroupList();
                // 重新加载会话列表
                this.getConversationListPanel().loadUserConversationsFromServer();
            }
        });
    }

    // 收到连接变更事件
    public void onReceiveConnectChange(String connectEvent) {
        if (isLogout) {
            return;
        }
        switch (connectEvent) {
            case ConnectionConstant.CONNECTING ->
                    this.headerPanel.getUserInfoPanel().updateOnlineStatus(OnlineStatus.OFFLINE);
            case ConnectionConstant.CONNECTED -> {
                this.headerPanel.getUserInfoPanel().updateOnlineStatus(OnlineStatus.ONLINE);
                this.loadData(); // 加载数据
            }
            case ConnectionConstant.DISCONNECTED -> {
                this.headerPanel.getUserInfoPanel().updateOnlineStatus(OnlineStatus.OFFLINE);
                this.getFriendListPanel().updateAllOfflineStatus();
                this.tokenManager.refreshTokenImmediately();
                this.imClientManager.reconnect(); // 调度重连
            }
            case ConnectionConstant.CONNECT_FAILED -> {
                this.headerPanel.getUserInfoPanel().updateOnlineStatus(0);
                this.getFriendListPanel().updateAllOfflineStatus();
                this.tokenManager.refreshTokenImmediately();
                this.imClientManager.reconnect(); // 调度重连
            }
        }
    }

    public void onReceiveOfflineDeviceMessage(OfflineDeviceMessage offlineDeviceMessage) {
        this.isLogout = true;
        if (offlineDeviceMessage.getOfflineType() == OfflineDeviceType.SQUEEZE_OUT) {
            this.imClientManager.disconnect();
            this.headerPanel.getUserInfoPanel().updateOnlineStatus(OnlineStatus.OFFLINE);
            JOptionPane.showMessageDialog(this, "您已被挤下线", "提醒", JOptionPane.WARNING_MESSAGE);
            new LoginFrame().setVisible(true);
            super.dispose();
        }
    }

    // 收到已读消息回执
    public void onReceiveReadReceiptNotify(ReadReceiptNotify readReceiptNotify) {
        this.getConversationListPanel().updateConversationRead(readReceiptNotify);
        this.getChatPanelWrapper().updateMessageRead(readReceiptNotify);
    }


    // 收到聊天消息时的处理实现
    public void onReceiveChatMessage(List<ChatMessage> chatMessages) {
        if (CollectionUtils.isEmpty(chatMessages)) {
            return;
        }
        for (ChatMessage chatMessage : chatMessages) {
            Message message = MessageConverter.convertToClientMessage(chatMessage);
            SwingUtilities.invokeLater(() -> {
                if (message.getSenderId() == currentUser.getUid() && message.getReceiverId() == currentUser.getUid()) {
                    // 自己收到自己的消息，无需再次加载，因为在发送的时候已经加载过一次了，不然会数据重复；
                    return;
                }

                // 保存到本地数据库
                this.getDatabaseManager().saveMessage(message);

                // 加载消息到会话窗口
                this.getChatPanelWrapper().addMessageToUI(message);

                // 如果正在对话中，清除未读消息数，否则增加未读消息数量
                if (this.getChatPanelWrapper().isCurrentChatPanelOpen(message.getConversationId())) {
                    this.getConversationListPanel().updateConversationWhenReceiveMessage(message, true);
                } else {
                    this.getConversationListPanel().updateConversationWhenReceiveMessage(message, false);
                }

                // 如果本地的用户会话被删除，需要从服务器中重新拉取
                if (!this.getConversationListPanel().existConversation(message.getConversationId())) {
                    this.getConversationListPanel().loadUserConversationsFromServer(message.getConversationId());
                }
            });
        }
    }

    public void saveLastSyncSeq(Long syncSeq) {
        this.getDatabaseManager().updateLastSyncSeq(this.currentUser.getUid(), syncSeq);
    }

    public void showError(String errorMsg) {
        JOptionPane.showMessageDialog(this, errorMsg, "失败", JOptionPane.ERROR_MESSAGE);
    }

    // getter

    public String getNetworkType() {
        return networkType;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public PopupMenuUtil getPopupMenuLUtil() {
        return popupMenuUtil;
    }

    public UserInfo getCurrentUser() {
        return currentUser;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public IMClientManager getImClientManager() {
        return imClientManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConversationListPanel getConversationListPanel() {
        return this.contentPanel.getLeftPanel().getConversationListPanel();
    }

    public FriendListPanel getFriendListPanel() {
        return this.contentPanel.getLeftPanel().getFriendListPanel();
    }

    public GroupListPanel getGroupListPanel() {
        return this.contentPanel.getLeftPanel().getGroupListPanel();
    }

    public RightPanel getChatPanelWrapper() {
        return this.contentPanel.getRightPanel();
    }

    public boolean isLogout() {
        return isLogout;
    }
}

