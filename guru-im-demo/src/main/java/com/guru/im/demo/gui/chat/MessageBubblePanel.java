package com.guru.im.demo.gui.chat;

import com.guru.im.common.constant.MessageType;
import com.guru.im.demo.gui.chat.bubble.*;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserConversation;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.UserService;
import com.guru.im.demo.util.ImageUtil;
import com.guru.im.protocol.model.ConversationType;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageBubblePanel extends JPanel {
    private final ChatPanel chatPanel;
    private Message message;
    private final UserInfo currentUser;
    private final UserConversation userConversation;

    private final JLabel timeLabel = new JLabel();
    private JPanel contentPanel;
    private JButton resendBtn;
    private AbstractChatBubble contentBubble;
    private final JLabel statusLabel = new JLabel();
    private final JLabel readStatusLabel = new JLabel();


    public MessageBubblePanel(ChatPanel chatPanel, Message message, UserInfo currentUser, UserConversation userConversation) {
        this.chatPanel = chatPanel;
        this.message = message;
        this.currentUser = currentUser;
        this.userConversation = userConversation;

        // 启用双缓冲
        setDoubleBuffered(true);
        setOpaque(true);
        setBackground(new Color(255, 255, 255));
        setLayout(new BorderLayout(0, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        initializeComponents();
        setupContextMenu();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 清除背景
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
    }

    private AbstractChatBubble createMediaContentBubble(Message message) {
        if (message.getMessageType() == MessageType.IMAGE) {
            return new ImageChatBubble(this.chatPanel, message, isOwnMessage());
        } else if (message.getMessageType() == MessageType.VIDEO) {
            return new VideoChatBubble(this.chatPanel, message, isOwnMessage());
        } else {
            return new FileChatBubble(this.chatPanel, message, isOwnMessage());
        }
    }

    public void refreshMessageStatusUI() {
        refreshMessageSendStatusUI();
        updateReadStatusUI();
    }

    private void initializeComponents() {
        // 时间标签
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeText;

        if (isOwnMessage()) {
            timeText = sdf.format(new Date(message.getClientSendTime())) + "  我";
            timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        } else {
            timeText = UserService.getUserName(message.getSenderId()) + "  " + sdf.format(new Date(message.getClientSendTime()));
            timeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        }

        timeLabel.setText(timeText);
        timeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        // 初始化ChatBubble
        if (message.getMessageType() == MessageType.TEXT) {
            contentBubble = new TextChatBubble(message.getMessageContent(), isOwnMessage());
        } else if (message.getMessageType() == MessageType.IMAGE
                || message.getMessageType() == MessageType.VIDEO) {
            contentBubble = createMediaContentBubble(message);
        } else {
            contentBubble = new FileChatBubble(this.chatPanel, message, isOwnMessage());
        }

        // 状态区域
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setOpaque(false);


        // 状态
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        statusLabel.setForeground(Color.GRAY);

        // 更新状态文本和颜色
        refreshMessageSendStatusUI();

        statusPanel.add(statusLabel);

        if (isOwnMessage()) {
            statusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

            contentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            contentPanel.setBorder(BorderFactory.createEmptyBorder());
            contentPanel.setOpaque(false);

            resendBtn = createResendBtn(); // 重发按钮

            contentPanel.add(resendBtn);
            contentPanel.add(contentBubble);

            readStatusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            readStatusLabel.setForeground(Color.GRAY);
            updateReadStatusUI();

            statusPanel.add(readStatusLabel);
        }

        // 组装组件
        add(timeLabel, BorderLayout.NORTH);

        // 根据消息方向设置对齐
        JPanel alignmentPanel = new JPanel(new FlowLayout(isOwnMessage() ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        alignmentPanel.setOpaque(false);
        if (isOwnMessage() && contentPanel != null) {
            alignmentPanel.add(contentPanel);
        } else {
            alignmentPanel.add(contentBubble);
        }

        JPanel bubbleContainer = new JPanel(new BorderLayout());
        bubbleContainer.setOpaque(false);
        bubbleContainer.add(alignmentPanel, BorderLayout.CENTER);
        bubbleContainer.add(statusPanel, BorderLayout.SOUTH);

        add(bubbleContainer, BorderLayout.CENTER);
    }

    private JButton createResendBtn() {
        JButton resendBtn = new JButton();
        resendBtn.setIcon(ImageUtil.createSVGIcon("image/resend.svg", 20));
        resendBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16));
        resendBtn.setToolTipText("点击重发");
        resendBtn.setBackground(null);
        resendBtn.setForeground(Color.BLACK);
        resendBtn.setContentAreaFilled(false); // 不绘制内容区域
        resendBtn.setFocusPainted(false);
        resendBtn.setBorderPainted(false);
        resendBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        resendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resendBtn.setVisible(false);
        resendBtn.addActionListener((event) -> {
            chatPanel.resendMessage(message);
            resendBtn.setVisible(false);
        });
        return resendBtn;
    }

    public void updateUploadProgress(int progress) {
        ((AbstractMediaChatBubble) contentBubble).setUploadProgress(progress);
        if (progress == 100) {
            repaint();
        }
    }

    public void updateDownloadProgress(int progress) {
        ((AbstractMediaChatBubble) contentBubble).setDownloadProgress(progress);
    }

    public void completeDownload() {
        ((AbstractMediaChatBubble) contentBubble).completeDownload();
        repaint();
    }

    public void reloadLocalThumbnail() {
        ((AbstractMediaChatBubble) contentBubble).reloadLocalThumbnail();
    }

    public void uploadFailed() {
        ((AbstractMediaChatBubble) contentBubble).uploadFailed();
    }

    private void setupContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("复制");
        JMenuItem deleteItem = new JMenuItem("删除");

        copyItem.addActionListener(e -> {
            String plainText = message.getMessageContent();
            StringSelection selection = new StringSelection(plainText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
        });

        deleteItem.addActionListener(e -> {
            firePropertyChange("messageDeleted", null, message);
        });

        popupMenu.add(copyItem);
        popupMenu.add(deleteItem);

        contentBubble.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    public void refreshMessageSendStatusUI() {
        SwingUtilities.invokeLater(() -> {
            int sendStatus = message.getSendStatus();
            if (sendStatus == Message.SEND_STATUS_UPLOADING) {
                statusLabel.setText("上传中...");
                statusLabel.setForeground(Color.ORANGE);
            } else if (sendStatus == Message.SEND_STATUS_UPLOAD_FAILED) {
                statusLabel.setText("上传失败");
                statusLabel.setForeground(Color.GRAY);
                uploadFailed();
            } else if (sendStatus == Message.SEND_STATUS_UPLOAD_PAUSE) {
                statusLabel.setText("已暂停上传");
                statusLabel.setForeground(Color.GRAY);
            } else if (sendStatus == Message.SEND_STATUS_DOWNLOADING) {
                statusLabel.setText("下载中...");
                statusLabel.setForeground(Color.ORANGE);
            } else if (sendStatus == Message.SEND_STATUS_DOWNLOAD_PAUSE) {
                statusLabel.setText("已暂停下载");
                statusLabel.setForeground(Color.GRAY);
            } else if (sendStatus == Message.SEND_STATUS_DOWNLOAD_FAILED) {
                statusLabel.setText("下载失败");
                statusLabel.setForeground(Color.GRAY);
                uploadFailed();
            } else if (sendStatus == Message.SEND_STATUS_DOWNLOADED) {
                statusLabel.setText("已下载");
                statusLabel.setForeground(Color.GRAY);
            } else if (sendStatus == Message.SEND_STATUS_FAILURE) {
                statusLabel.setText("发送失败");
                statusLabel.setForeground(Color.GRAY);
                if (resendBtn != null) {
                    resendBtn.setVisible(true);
                }
            } else if (sendStatus == Message.SEND_STATUS_SENDING) {
                statusLabel.setText("发送中...");
                statusLabel.setForeground(Color.ORANGE);
            } else {
                statusLabel.setText("");
            }
            repaint();
        });
    }

    public void updateReadStatusUI() {
        SwingUtilities.invokeLater(() -> {
            if (isOwnMessage()) {
                // 当前得设计，私聊才有已读状态
                if (message.getSendStatus() == Message.SEND_STATUS_SUCCESS
                        && userConversation.getConversationType() == ConversationType.PRIVATE_VALUE) {
                    readStatusLabel.setVisible(true);
                } else {
                    readStatusLabel.setVisible(false);
                }
                readStatusLabel.setText(this.message.getReadStatus() == Message.READ_STATUS_READ ? "已读" : "未读");

                repaint();
            }
        });
    }

    private ImageIcon getIcon(String path) {
        java.net.URL url = getClass().getClassLoader().getResource(path);
        if (url != null) {
            ImageIcon icon = new ImageIcon(url);
            Image scaledImage = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }
        return null;
    }

    private boolean isOwnMessage() {
        return message != null && message.getSenderId() == currentUser.getUid();
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public Dimension getPreferredSize() {
        // 计算合适的尺寸
        Dimension bubbleSize = contentBubble.getPreferredSize();

        int width = Math.max(bubbleSize.width, timeLabel.getPreferredSize().width);
        int height = bubbleSize.height + 55; // 包含时间、内容和状态的高度

        return new Dimension(width, height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}