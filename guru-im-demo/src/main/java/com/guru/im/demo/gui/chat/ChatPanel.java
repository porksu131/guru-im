package com.guru.im.demo.gui.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.im.demo.convert.MessageBuilder;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.gui.chat.listener.FileDownloadProgressListener;
import com.guru.im.demo.gui.chat.listener.FileUploadProgressListener;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.gui.component.RoundedTextField;
import com.guru.im.demo.gui.file.FileTransferManager;
import com.guru.im.demo.gui.file.model.FileTransfer;
import com.guru.im.demo.model.MediaInfo;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserConversation;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.sqlite.DatabaseManager;
import com.guru.im.demo.util.ButtonUtil;
import com.guru.im.demo.util.FileUtil;
import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatPanel extends JPanel {
    private final MainFrame mainFrame;
    private final UserInfo currentUser;
    private RoundedTextField messageField;
    private final UserConversation userConversation;
    private final Long conversationId;

    // 数据库管理
    private final DatabaseManager databaseManager;

    // 消息容器
    private final JPanel messagesContainer;
    private final JScrollPane scrollPane;
    private final List<MessageBubblePanel> messagePanels = new ArrayList<>();


    // 消息发送相关
    private final MessageSender messageSender;
    private final MessageReceiver messageReceiver;

    // 文件相关
    private final FileTransferManager fileTransferManager;
    private final FileUploadProgressListener fileUploadProgressListener;
    private final FileDownloadProgressListener fileDownloadProgressListener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 分页加载
    private static final int LOAD_SIZE = 20;
    private boolean hasMore = true;
    private int loadedCount = 0;

    // 头部菜单
    private JPopupMenu mainMenuPopup;

    // 消息排序器
    private final Comparator<Message> messageComparator = new MessageComparator();

    public ChatPanel(MainFrame mainFrame, UserConversation userConversation) {
        super(new BorderLayout());
        this.mainFrame = mainFrame;
        this.currentUser = mainFrame.getCurrentUser();
        this.userConversation = userConversation;
        this.databaseManager = mainFrame.getDatabaseManager();
        this.conversationId = userConversation.getConversationId();

        setDoubleBuffered(true); // 启用双缓冲
        setBorder(BorderFactory.createEmptyBorder());
        //setBackground(new Color(240, 240, 240));
        setBackground(new Color(255, 255, 255));

        // 头部信息栏
        JPanel headerPanel = createHeaderPanel();

        // 创建消息容器
        messagesContainer = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 确保清除背景
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false; // 禁用优化绘制，防止重叠问题
            }
        };
        messagesContainer.setDoubleBuffered(true);
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(new Color(255, 255, 255, 225));
        messagesContainer.setFocusable(Boolean.FALSE);

        // 滚动面板
        scrollPane = new JScrollPane(messagesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // 使用简单滚动模式
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 223, 230)));
        scrollPane.setFocusable(Boolean.FALSE);
        // 输入面板
        JPanel inputPanel = createInputPanel();

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // 初始化消息发送接收器
        this.messageSender = new MessageSender(mainFrame, userConversation);
        this.messageReceiver = new MessageReceiver();

        // 设置消息消费者
        this.messageReceiver.setMessageConsumer(this::addMessageToUI);
        this.messageSender.setMessageStatusUpdater(this::updateMessage);

        // 初始化文件上传管理器
        this.fileTransferManager = new FileTransferManager(mainFrame.getCurrentUser(), mainFrame.getDatabaseManager());
        this.fileUploadProgressListener = new FileUploadProgressListener(this);
        this.fileDownloadProgressListener = new FileDownloadProgressListener(this);

        // 加载初始消息
        loadInitialMessages();

        // 添加滚动监听用于分页加载
        setupScrollListener();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(248, 249, 250));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 40));
        // 左侧空白
        JPanel leftSpacer = new JPanel();
        leftSpacer.setOpaque(false);

        // 聊天名称标签
        JLabel chatNameLabel = new JLabel(userConversation.getShowName(), SwingConstants.CENTER);
        chatNameLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        chatNameLabel.setForeground(new Color(50, 50, 50));

        // 右侧按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JButton phoneButton = ButtonUtil.createSvgButton("image/phone.svg", "语音通话", 16, Color.LIGHT_GRAY);
        phoneButton.setRolloverIcon(ImageUtil.createSVGIcon("image/phone.svg", 16, Color.GRAY));

        JButton videoButton = ButtonUtil.createSvgButton("image/video.svg", "视频通话", 16, Color.LIGHT_GRAY);
        videoButton.setRolloverIcon(ImageUtil.createSVGIcon("image/video.svg", 16, Color.GRAY));

        JButton screenButton = ButtonUtil.createSvgButton("image/screen.svg", "桌面共享", 16, Color.LIGHT_GRAY);
        screenButton.setRolloverIcon(ImageUtil.createSVGIcon("image/screen.svg", 16, Color.GRAY));

        JButton menuButton = ButtonUtil.createSvgButton("image/menu-dots.svg", "更多", 16, Color.LIGHT_GRAY);
        menuButton.setRolloverIcon(ImageUtil.createSVGIcon("image/menu-dots.svg", 16, Color.GRAY));
        menuButton.addActionListener((e) -> {
            showMainMenuPopup(menuButton);
        });

        JButton closeButton = ButtonUtil.createSvgButton("image/close.svg", "关闭", 16, Color.LIGHT_GRAY);
        closeButton.setRolloverIcon(ImageUtil.createSVGIcon("image/close.svg", 16, Color.RED));
        closeButton.addActionListener((e) -> {
            this.mainFrame.getChatPanelWrapper().closeChatPanel();
        });
        buttonPanel.add(phoneButton);
        buttonPanel.add(videoButton);
        buttonPanel.add(screenButton);
        buttonPanel.add(menuButton);
        buttonPanel.add(closeButton);

        headerPanel.add(leftSpacer, BorderLayout.WEST);
        headerPanel.add(chatNameLabel, BorderLayout.CENTER);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        inputPanel.setBackground(Color.WHITE);

        // 添加上传按钮面板
        JPanel uploadButtonPanel = createUploadButtonPanel();
        inputPanel.add(uploadButtonPanel, BorderLayout.NORTH);

        // 使用圆角文本框
        messageField = new RoundedTextField();
        // 设置提示文本
        messageField.setPlaceholder("输入消息 ...");

        // 发送按钮
        JButton sendBtn = new JButton("发送");
        sendBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        sendBtn.setBackground(new Color(0, 150, 136));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.addActionListener(e -> sendTextMessage());
        sendBtn.setPreferredSize(new Dimension(80, 45));
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 回车发送
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendTextMessage();
                }
            }
        });

        // 输入区域面板
        JPanel inputFieldPanel = new JPanel(new BorderLayout(10, 0));
        inputFieldPanel.setBackground(Color.WHITE);
        inputFieldPanel.setBorder(BorderFactory.createEmptyBorder());

        inputFieldPanel.add(messageField, BorderLayout.CENTER);
        inputFieldPanel.add(sendBtn, BorderLayout.EAST);

        inputPanel.add(inputFieldPanel, BorderLayout.CENTER);

        return inputPanel;
    }

    private JPanel createUploadButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);

        // 图片按钮
        JButton imageButton = ButtonUtil.createIconButton("image/upload-image.png", "发送图片");
        imageButton.addActionListener(e -> selectAndUploadFiles(FileUtil.FILE_TYPE_IMAGE));

        // 语音按钮
        JButton voiceButton = ButtonUtil.createIconButton("image/upload-voice.png", "发送语音");
        voiceButton.addActionListener(e -> selectAndUploadFiles(FileUtil.FILE_TYPE_AUDIO));

        // 视频按钮
        JButton videoButton = ButtonUtil.createIconButton("image/upload-video.png", "发送视频");
        videoButton.addActionListener(e -> selectAndUploadFiles(FileUtil.FILE_TYPE_VIDEO));

        // 文件按钮
        JButton fileButton = ButtonUtil.createIconButton("image/upload-file.png", "发送文件");
        fileButton.addActionListener(e -> selectAndUploadFiles(FileUtil.FILE_TYPE_OTHER));

        buttonPanel.add(imageButton);
        buttonPanel.add(voiceButton);
        buttonPanel.add(videoButton);
        buttonPanel.add(fileButton);

        return buttonPanel;
    }

    private void setupScrollListener() {
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (e.getValue() == 0 && !e.getValueIsAdjusting()) {
                loadHistoryData();
            }
        });
    }

    // 事件监听 end

    private void loadInitialMessages() {
        // 从数据库加载当前会话数据
        long maxClientSeq = databaseManager.getMaxClientSeq();
        messageSender.initializeClientSeq(maxClientSeq);

        List<Message> recentMessages = databaseManager.loadMessages(
                conversationId, Long.MAX_VALUE, LOAD_SIZE
        );

        if (recentMessages.size() < LOAD_SIZE) {
            hasMore = false;
        }

        if (recentMessages.isEmpty()) {
            // 清空容器
            messagesContainer.removeAll();
            messagePanels.clear();
        }

        loadedCount = recentMessages.size();
        addMessagesToUI(recentMessages, true);
        scrollToBottom();
    }

    // 加载历史消息
    public void loadHistoryData() {
        if (hasMore) {
            long sequenceId = getOldestSequenceId();
            List<Message> historyMessages = databaseManager.loadMessages(conversationId, sequenceId, LOAD_SIZE);

            if (historyMessages.size() < LOAD_SIZE) {
                hasMore = false;
            }

            loadedCount += historyMessages.size();
            addMessagesToUI(historyMessages, false); // 添加到顶部

            // 保持滚动位置
            SwingUtilities.invokeLater(() -> {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                int currentPosition = verticalBar.getValue();
                int addedHeight = calculateAddedHeight(historyMessages);
                verticalBar.setValue(currentPosition + addedHeight);
            });
        }
    }

    private int calculateAddedHeight(List<Message> messages) {
        int totalHeight = 0;
        for (Message message : messages) {
            // 估算每个消息的高度
            totalHeight += 80; // 大致高度，可以根据需要调整
        }
        return totalHeight;
    }

    // 获取最旧的消息序列号（用于分页加载）
    public long getOldestSequenceId() {
        if (messagePanels.isEmpty()) {
            return 0;
        }

        long oldestSeq = Long.MAX_VALUE;
        for (MessageBubblePanel panel : messagePanels) {
            Message msg = panel.getMessage();
            if (msg.getServerSeq() != -1 && msg.getServerSeq() < oldestSeq) {
                oldestSeq = msg.getServerSeq();
            }
        }

        return oldestSeq == Long.MAX_VALUE ? 0 : oldestSeq;
    }

    private void addMessagesToUI(List<Message> messages, boolean addToBottom) {
        // 先对消息进行排序
        List<Message> sortedMessages = new ArrayList<>(messages);
        sortedMessages.sort(messageComparator);
        for (Message message : sortedMessages) {
            MessageBubblePanel bubblePanel = createMessageBubblePanel(message);

            if (addToBottom) {
                messagesContainer.add(bubblePanel);
                messagePanels.add(bubblePanel);
            } else {
                messagesContainer.add(bubblePanel, 0);
                messagePanels.add(0, bubblePanel);
            }
        }

        messagesContainer.revalidate();
        messagesContainer.repaint();
    }

    private MessageBubblePanel createMessageBubblePanel(Message message) {
        MessageBubblePanel bubblePanel = new MessageBubblePanel(this, message, currentUser, userConversation);
        bubblePanel.addPropertyChangeListener("messageDeleted", evt -> {
            Message deletedMessage = (Message) evt.getNewValue();
            deleteMessage(deletedMessage);
        });
        return bubblePanel;
    }

    // 根据clientMsgId获取消息实体
    public Message getMessageByClientMsgId(String clientMsgId) {
        // 查找并更新对应的消息
        for (MessageBubblePanel panel : messagePanels) {
            if (panel.getMessage().getClientMsgId().equals(clientMsgId)) {
                return panel.getMessage();
            }
        }
        return null;
    }

    // 重新排序所有消息
    private void reSortMessages() {
        // 收集所有消息
        List<Message> allMessages = new ArrayList<>();
        for (MessageBubblePanel panel : messagePanels) {
            allMessages.add(panel.getMessage());
        }

        // 清空容器
        messagesContainer.removeAll();
        messagePanels.clear();

        // 重新添加排序后的消息
        addMessagesToUI(allMessages, true);

        scrollToBottom();
    }

    // 消息状态更新
    public void updateMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            this.databaseManager.saveMessage(message);
            this.refreshMessageStatusUI(message);
        });
    }

    private void refreshMessageStatusUI(Message message) {
        SwingUtilities.invokeLater(() -> {
            MessageBubblePanel panel = findMessageByClientMsgId(message.getClientMsgId());
            if (panel != null) {
                panel.refreshMessageStatusUI();
            }

            // 重新排序所有消息
            reSortMessages();
        });
    }

    // 更新消息状态
    private void updateMessageSendStatus(String clientMsgId, int sendStatus) {
        SwingUtilities.invokeLater(() -> {
            MessageBubblePanel panel = findMessageByClientMsgId(clientMsgId);
            if (panel != null) {
                panel.getMessage().setSendStatus(sendStatus);
                this.databaseManager.saveMessage(panel.getMessage());
                panel.refreshMessageSendStatusUI();
            }
        });
    }

    public MessageBubblePanel findMessageByClientMsgId(String clientMsgId) {
        for (MessageBubblePanel panel : messagePanels) {
            if (panel.getMessage().getClientMsgId().equals(clientMsgId)) {
                return panel;
            }
        }
        return null;
    }

    // 更新已读状态
    public void updateMessageRead(Long readSeq) {
        SwingUtilities.invokeLater(() -> {
            for (MessageBubblePanel panel : messagePanels) {
                Message msg = panel.getMessage();
                if (msg.getServerSeq() <= readSeq && msg.getReadStatus() == Message.READ_STATUS_UNREAD) {
                    msg.setReadStatus(Message.READ_STATUS_READ);
                    this.databaseManager.saveMessage(msg);
                    panel.updateReadStatusUI();
                }
            }
            repaint();
        });
    }

    public void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public boolean conversationIdEquals(Long conversationId) {
        return this.conversationId.equals(conversationId);
    }

    public void clearMessageInput() {
        this.messageField.setText("");
    }


    // 文件上传相关 begin =============================================================================================

    // 开始上传
    private void selectAndUploadFiles(int fileType) {
        FileUtil.selectFilesNonModal(this, fileType, new FileUtil.FileSelectionCallback() {
            @Override
            public void onFilesSelected(File[] selectedFiles) {
                for (File file : selectedFiles) {
                    // 初始化文件传输对象
                    FileTransfer transfer = new FileTransfer(fileType, file);
                    // 初始化消息对象
                    Message fileMessage = MessageBuilder.buildFileMessage(transfer, currentUser, userConversation);
                    fileMessage.setSendStatus(Message.SEND_STATUS_UPLOADING);

                    // 关联消息
                    transfer.setClientMsgId(fileMessage.getClientMsgId());
                    transfer.setConversationId(fileMessage.getConversationId());
                    fileMessage.setUploadTransfer(transfer);

                    // 添加到UI
                    addMessageToUI(fileMessage);

                    // 保存到数据库
                    databaseManager.saveMessage(fileMessage);
                    databaseManager.saveFileTransfer(transfer);
                    // 启动上传
                    fileTransferManager.startUpload(transfer, fileUploadProgressListener);
                }
            }
        });
    }

    // 处理本地传输对象准备完成事件，已准备好需IO操作的md5和contentType
    public void handleCompletePrepareTransfer(FileTransfer transfer) {
        SwingUtilities.invokeLater(() -> {
            // 保存进度
            this.databaseManager.saveFileTransfer(transfer);

            // 更新消息绑定的文件信息
            Message fileMessage = getMessageByClientMsgId(transfer.getClientMsgId());
            MediaInfo mediaInfo = fileMessage.getMediaInfo();
            mediaInfo.setMimeType(transfer.getContentType());

            try {
                fileMessage.setMessageContent(objectMapper.writeValueAsString(mediaInfo));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            // 保存消息绑定的文件信息
            this.databaseManager.saveMessage(fileMessage);
        });
    }

    // 处理本地缩略图完成生成事件
    public void handleCompleteGenerateThumbnail(FileTransfer transfer) {
        SwingUtilities.invokeLater(() -> {
            // 保存进度
            this.databaseManager.saveFileTransfer(transfer);

            // 更新消息绑定的文件信息
            Message fileMessage = getMessageByClientMsgId(transfer.getClientMsgId());
            MediaInfo mediaInfo = fileMessage.getMediaInfo();
            mediaInfo.setLocalThumbnailPath(transfer.getThumbnailPath());
            if (transfer.getFileType() == FileUtil.FILE_TYPE_IMAGE) {
                mediaInfo.setImageWidth(transfer.getOriginWidth());
                mediaInfo.setImageHeight(transfer.getOriginHeight());
            } else if (transfer.getFileType() == FileUtil.FILE_TYPE_VIDEO) {
                mediaInfo.setVideoWidth(transfer.getOriginWidth());
                mediaInfo.setVideoHeight(transfer.getOriginHeight());
                mediaInfo.setVideoBitrate(transfer.getVideoBitrate());
                mediaInfo.setVideoDuration(transfer.getDuration());
                mediaInfo.setVideoCodec(transfer.getVideoCodec());
                mediaInfo.setVoiceCodec(transfer.getAudioCodec());
                mediaInfo.setVoiceBitrate(transfer.getAudioBitrate());
            }

            try {
                fileMessage.setMessageContent(objectMapper.writeValueAsString(mediaInfo));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            // 保存消息绑定的文件信息
            this.databaseManager.saveMessage(fileMessage);

            // 重新加载新的缩略图到界面
            MessageBubblePanel panel = findMessageByClientMsgId(transfer.getClientMsgId());
            if (panel != null) {
                panel.reloadLocalThumbnail();
            }
        });
    }

    // 处理上传进度变更事件
    public void handleFileUploadProgressChanged(FileTransfer transfer) {
        SwingUtilities.invokeLater(() -> {
            // 保存进度
            this.databaseManager.saveFileTransfer(transfer);
            // 查找并更新对应的消息
            for (MessageBubblePanel panel : messagePanels) {
                if (panel.getMessage().getClientMsgId().equals(transfer.getClientMsgId())) {
                    panel.updateUploadProgress(transfer.getProgress());
                    break;
                }
            }
        });
    }

    // 处理上传完成事件
    public void handleFileUploadComplete(FileTransfer transfer) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 保存进度
                this.databaseManager.saveFileTransfer(transfer);
                // 更新进度条
                this.handleFileUploadProgressChanged(transfer);
                // 更新文件的下载url信息，并将文件信息序列化成json，发送时再保存
                Message fileMessage = getAndFillDownloadUrlToFileMessage(transfer);
                // 更新为发送中
                fileMessage.setSendStatus(Message.SEND_STATUS_SENDING);
                updateMessageSendStatus(fileMessage.getClientMsgId(), Message.SEND_STATUS_SENDING);
                // 上传完成，自动发送消息给好友
                this.messageSender.sendMessage(fileMessage);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "文件发送失败: " + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }

        });
    }

    // 处理上传错误事件
    public void handleUploadFileError(FileTransfer transfer, String errorMsg) {
        this.databaseManager.saveFileTransfer(transfer);
        updateMessageSendStatus(transfer.getClientMsgId(), Message.SEND_STATUS_UPLOAD_FAILED);
        JOptionPane.showMessageDialog(this, "文件上传失败: " + errorMsg,
                "错误", JOptionPane.ERROR_MESSAGE);
    }

    // 暂停上传
    public void pauseUpload(Message message) {
        updateMessageSendStatus(message.getClientMsgId(), Message.SEND_STATUS_UPLOAD_PAUSE);
        fileTransferManager.pauseUpload(message.getUploadTransfer());
    }

    // 暂停下载
    public void resumeUpload(Message message) {
        updateMessageSendStatus(message.getClientMsgId(), Message.SEND_STATUS_UPLOADING);
        fileTransferManager.resumeUpload(message.getUploadTransfer(), this.fileUploadProgressListener);
    }

    private Message getAndFillDownloadUrlToFileMessage(FileTransfer transfer) {
        Message message = getMessageByClientMsgId(transfer.getClientMsgId());
        if (message == null) {
            throw new RuntimeException("message " + transfer.getClientMsgId() + " not found");
        }
        MediaInfo mediaInfo = message.getMediaInfo();
        mediaInfo.setFileId(transfer.getFileId());
        // 设置消息类型和文件相关信息
        switch (transfer.getFileType()) {
            case FileUtil.FILE_TYPE_IMAGE:
            case FileUtil.FILE_TYPE_VIDEO:
                mediaInfo.setFileUrl(transfer.getFileUrl());
                mediaInfo.setThumbnailUrl(transfer.getThumbnailUrl());
                break;

            default:
                mediaInfo.setFileUrl(transfer.getFileUrl());
                break;
        }
        try {
            message.setMessageContent(objectMapper.writeValueAsString(mediaInfo));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return message;
    }


    // 启动下载
    public void startDownload(FileTransfer fileTransfer) {
        this.updateMessageSendStatus(fileTransfer.getClientMsgId(), Message.SEND_STATUS_DOWNLOADING);
        this.databaseManager.saveFileTransfer(fileTransfer);
        fileTransferManager.startDownload(fileTransfer, fileDownloadProgressListener);
    }

    // 处理下载进度
    public void handleFileDownloadProgressChanged(FileTransfer transfer, int progress, double speed) {
        SwingUtilities.invokeLater(() -> {
            // 查找并更新对应的消息
            for (MessageBubblePanel panel : messagePanels) {
                if (panel.getMessage().getClientMsgId().equals(transfer.getClientMsgId())) {
                    panel.updateDownloadProgress(progress);
                    break;
                }
            }
        });
    }

    // 处理下载完成事件
    public void handleFileDownloadComplete(FileTransfer transfer) {
        SwingUtilities.invokeLater(() -> {
            // 查找并更新对应的消息
            for (MessageBubblePanel panel : messagePanels) {
                if (panel.getMessage().getClientMsgId().equals(transfer.getClientMsgId())) {
                    panel.getMessage().setSendStatus(Message.SEND_STATUS_DOWNLOADED);
                    panel.completeDownload();
                    databaseManager.saveMessage(panel.getMessage());
                    panel.refreshMessageSendStatusUI();
                    break;
                }
            }

        });
    }

    // 处理下载错误事件
    public void handleFileDownloadFailed(FileTransfer transfer, String errorMsg) {
        transfer.setStatus(FileTransfer.Status.FAILED);
        this.databaseManager.saveFileTransfer(transfer);
        updateMessageSendStatus(transfer.getClientMsgId(), Message.SEND_STATUS_DOWNLOAD_FAILED);
        JOptionPane.showMessageDialog(this, "下载失败: " + errorMsg,
                "错误", JOptionPane.ERROR_MESSAGE);
    }

    // 暂停下载
    public void pauseDownload(Message message) {
        updateMessageSendStatus(message.getClientMsgId(), Message.SEND_STATUS_DOWNLOAD_PAUSE);
        fileTransferManager.pauseDownload(message.getDownloadTransfer());
    }

    // 恢复下载
    public void resumeDownload(Message message) {
        updateMessageSendStatus(message.getClientMsgId(), Message.SEND_STATUS_DOWNLOADING);
        fileTransferManager.resumeDownload(message.getDownloadTransfer(), this.fileDownloadProgressListener);
    }

    // 文件上传相关 end ===============================================================================================


    // 消息收发 begin=================================================================================================

    // 发送消息
    public void sendTextMessage() {
        String textMessage = messageField.getText().trim();
        if (!textMessage.isEmpty()) {
            Message sendMsg = MessageBuilder.buildTextMessage(textMessage, currentUser, userConversation);

            // 添加到UI
            addMessageToUI(sendMsg);

            // 发送消息
            messageSender.sendMessage(sendMsg);

            // 清空输入框
            clearMessageInput();
        }
    }

    // 接收消息
    public void addMessageToUI(Message message) {
        SwingUtilities.invokeLater(() -> {
            // 检查是否已经存在该消息（避免重复添加）
            for (MessageBubblePanel panel : messagePanels) {
                if (panel.getMessage().getClientMsgId().equals(message.getClientMsgId())) {
                    return; // 已经存在，不重复添加
                }
            }

            MessageBubblePanel bubblePanel = createMessageBubblePanel(message);

            messagesContainer.add(bubblePanel);
            messagePanels.add(bubblePanel);

//            messagesContainer.revalidate();
//            messagesContainer.repaint();

            // 使用更高效的重绘方式
            messagesContainer.revalidate();
            Rectangle lastRect = bubblePanel.getBounds();
            messagesContainer.repaint(lastRect.x, lastRect.y, lastRect.width, lastRect.height);

            scrollToBottom();
        });
    }

    // 删除消息
    public void deleteMessage(Message message) {
        databaseManager.deleteMessage(message);
        // 从UI中移除
        for (int i = 0; i < messagePanels.size(); i++) {
            if (messagePanels.get(i).getMessage().equals(message)) {
                messagesContainer.remove(i);
                messagePanels.remove(i);
                break;
            }
        }
        messagesContainer.revalidate();
        messagesContainer.repaint();
    }

    // 单条重发
    public void resendMessage(Message failedMessage) {
        boolean isMeSend = failedMessage.getSenderId() == currentUser.getUid();
        if (isMeSend && failedMessage.getSendStatus() == Message.SEND_STATUS_FAILURE) {
            messageSender.resendMessage(failedMessage);
        }
    }

    // 批量重发
    public void resendAllFailedMessages() {
        List<Message> failedMessages = databaseManager.loadSendFailMessages(conversationId);
        messageSender.resendAllFailedMessages(failedMessages);
    }


    // 消息收发 end =====================================================================================================


    // 菜单按钮 begin ===================================================================================================
    private void showMainMenuPopup(Component invoker) {
        if (mainMenuPopup == null) {
            mainMenuPopup = new JPopupMenu();
            mainMenuPopup.setBorder(BorderFactory.createEmptyBorder());
            mainMenuPopup.setBorderPainted(false);

            // 菜单项列表
            String[] menuItems = {"查看资料", "清空历史", "查找历史", "静音通知"};
            String[] icons = {"image/user-info.svg", "image/delete.svg", "image/search.svg", "image/mute.svg"};

            for (int i = 0; i < menuItems.length; i++) {
                JMenuItem menuItem = new JMenuItem(menuItems[i]);
                menuItem.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
                menuItem.setIcon(ImageUtil.createSVGIcon(icons[i], 12, Color.BLACK));
                menuItem.setIconTextGap(10);
                menuItem.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                menuItem.setCursor(new Cursor(Cursor.HAND_CURSOR));
                menuItem.setBorderPainted(false);
                menuItem.setFocusPainted(false);

                final String action = menuItems[i];
                menuItem.addActionListener(e -> handleMenuAction(action));

                mainMenuPopup.add(menuItem);

                // 添加分隔线（最后一个不添加）
                if (i < menuItems.length - 1) {
                    mainMenuPopup.addSeparator();
                }
            }
        }

        mainMenuPopup.show(invoker, -70, invoker.getHeight() + 5);
    }

    private void handleMenuAction(String action) {
        switch (action) {
            case "查看资料":
                //showUserInfoDialog();
                break;
            case "清空历史":
                clearChatRecord();
                break;
            case "查找历史":
                //showFindChatRecordDialog();
                break;
            case "静音通知":
                break;
        }
    }

    public void clearChatRecord() {
        int result = JOptionPane.showConfirmDialog(this, "确定清空当前会话的所有历史消息吗？", "操作确认", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            Long conversationId = userConversation.getConversationId();
            // 还要先清除缩略图资源，待实现
            // 删除会话中的所有文件传输记录
            this.mainFrame.getDatabaseManager().deleteAllConversationFileTransfer(conversationId);
            // 删除会话中的所有消息
            this.mainFrame.getDatabaseManager().deleteAllConversationMessage(conversationId);
            // 更新用户会话的最后一条信息，但保留最后一条消息的序列号，用于后续的同步，避免再次拉取才删除的历史消息
            this.userConversation.setLastMessageContent("");
            this.userConversation.setLastMessageTime(null);
            this.userConversation.setLastMessageSender(null);
            this.mainFrame.getDatabaseManager().saveOrUpdateUserConversation(this.userConversation);
            this.mainFrame.getConversationListPanel().getConversationList().repaint();
            this.loadInitialMessages();
        }
    }
    // 菜单按钮 =========================================================================================================
}