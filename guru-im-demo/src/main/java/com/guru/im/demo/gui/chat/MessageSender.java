package com.guru.im.demo.gui.chat;

import com.guru.im.core.common.listener.ChatMessageSendCallBack;
import com.guru.im.demo.convert.MessageConverter;
import com.guru.im.demo.gui.MainFrame;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.model.UserConversation;
import com.guru.im.demo.sqlite.DatabaseManager;
import com.guru.im.protocol.model.ChatMessage;
import com.guru.im.protocol.model.ChatMessageAck;
import com.guru.im.sdk.IMClientManager;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MessageSender {
    private static final long MESSAGE_TIMEOUT = 5000;
    private static final long MAX_RETRY_COUNT = 1;

    private final AtomicLong nextClientSeq = new AtomicLong(1);
    private final BlockingQueue<Message> sendQueue = new PriorityBlockingQueue<>(11,
            Comparator.comparingLong(Message::getClientSeq));

    private Message pendingMessage;
    private final ConcurrentMap<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutChecker = Executors.newSingleThreadScheduledExecutor();

    private final IMClientManager imClientManager;
    private final DatabaseManager dbManager;
    private final UserConversation userConversation;
    private final MainFrame mainFrame;

    // 改为使用Consumer来通知消息状态变化
    private Consumer<Message> messageStatusUpdater;

    public MessageSender(MainFrame mainFrame, UserConversation userConversation) {
        this.mainFrame = mainFrame;
        this.imClientManager = mainFrame.getImClientManager();
        this.dbManager = mainFrame.getDatabaseManager();
        this.userConversation = userConversation;

        timeoutChecker.scheduleAtFixedRate(this::checkMessagesTimeout, 1, 1, TimeUnit.SECONDS);
    }

    public void initializeClientSeq(long maxSeq) {
        if (maxSeq > 0) {
            nextClientSeq.set(maxSeq);
        }
    }

    public void sendMessage(Message sendMsg) {
        sendMsg.setClientSeq(nextClientSeq.getAndIncrement() + 1);
        sendMsg.setClientSendTime(System.currentTimeMillis());

        // 保存到数据库
        dbManager.saveMessage(sendMsg);

        // 发送到服务器
        if (pendingMessage == null) {
            sendImmediately(sendMsg);
        } else {
            sendQueue.add(sendMsg);
        }
    }

    private void sendImmediately(Message msg) {
        pendingMessage = msg;
        pendingMessages.put(msg.getClientMsgId(), msg);
        SwingUtilities.invokeLater(() -> {
            sendChatMessageToServer(msg);
        });
    }

    public void handleAck(ChatMessageAck ackMessage) {
        Message msg = pendingMessages.remove(ackMessage.getClientMsgId());
        if (msg != null) {
            msg.setMessageId(ackMessage.getMessageId());
            msg.setServerSeq(ackMessage.getServerSeq());
            msg.setTimestamp(ackMessage.getTimestamp());
            msg.setSendStatus(Message.SEND_STATUS_SUCCESS);
            msg.setRetryCount(0);

            // 更新数据库
            dbManager.saveMessage(msg);

            // 更新会话列表的最后消息
            mainFrame.getConversationListPanel().updateConversationLastMessage(msg, userConversation);

            // 通知UI消息状态变化
            if (messageStatusUpdater != null) {
                messageStatusUpdater.accept(msg);
            }

            // 处理下一条消息
            pendingMessage = sendQueue.poll();
            if (pendingMessage != null) {
                sendImmediately(pendingMessage);
            }
        }
    }

    public void checkMessagesTimeout() {
        long currentTime = System.currentTimeMillis();

        for (Message msg : pendingMessages.values()) {
            if (currentTime - msg.getClientSendTime() > MESSAGE_TIMEOUT) {
                handleMessageTimeout(msg);
            }
        }
    }

    private void handleMessageTimeout(Message msg) {
        if (msg.getRetryCount() < MAX_RETRY_COUNT) {
            msg.setRetryCount(msg.getRetryCount() + 1);
            msg.setClientSendTime(System.currentTimeMillis());
            dbManager.saveMessage(msg);

            if (messageStatusUpdater != null) {
                messageStatusUpdater.accept(msg);
            }

            if (pendingMessage == null || pendingMessage.getClientSeq() == msg.getClientSeq()) {
                sendImmediately(msg);
            }
        } else {
            msg.setSendStatus(Message.SEND_STATUS_FAILURE);
            dbManager.saveMessage(msg);

            if (messageStatusUpdater != null) {
                messageStatusUpdater.accept(msg);
            }

            pendingMessages.remove(msg.getClientMsgId());

            if (pendingMessage != null && pendingMessage.getClientSeq() == msg.getClientSeq()) {
                pendingMessage = sendQueue.poll();
                if (pendingMessage != null) {
                    sendImmediately(pendingMessage);
                }
            }
        }
    }

    // 设置器方法
    public void setMessageStatusUpdater(Consumer<Message> messageStatusUpdater) {
        this.messageStatusUpdater = messageStatusUpdater;
    }

    public void shutdown() {
        timeoutChecker.shutdown();
    }

    // 单条重发
    public void resendMessage(Message msg) {
        if (msg.getSendStatus() == Message.SEND_STATUS_FAILURE) {
            msg.setSendStatus(Message.SEND_STATUS_SENDING);
            msg.setRetryCount(0);
            msg.setClientSendTime(System.currentTimeMillis());
            msg.setClientSeq(nextClientSeq.getAndIncrement());

            dbManager.saveMessage(msg);

            if (messageStatusUpdater != null) {
                messageStatusUpdater.accept(msg);
            }

            if (pendingMessage == null) {
                sendImmediately(msg);
            } else if (msg.getClientSeq() < pendingMessage.getClientSeq()) {
                sendQueue.add(pendingMessage);
                sendImmediately(msg);
            } else {
                sendQueue.add(msg);
            }
        }
    }

    // 批量重发
    public void resendAllFailedMessages(List<Message> failedMessages) {
        if (failedMessages.isEmpty()) return;
        failedMessages.sort(Comparator.comparingLong(Message::getClientSeq));

        for (Message msg : failedMessages) {
            resendMessage(msg);
        }
    }

    private void sendChatMessageToServer(Message sendMsg) {
        ChatMessage chatMessage = MessageConverter.convertToIMChatMessage(sendMsg, imClientManager.getDeviceInfo());
        try {
            this.imClientManager.chat().sendChatMessage(chatMessage, new ChatMessageSendCallBack() {
                @Override
                public void onSendSuccess(ChatMessageAck chatMessageAck) {
                    SwingUtilities.invokeLater(() -> {
                        handleAck(chatMessageAck);
                    });
                }

                @Override
                public void onSendFail(String message) {
                    SwingUtilities.invokeLater(() -> {
                        sendMsg.setSendStatus(Message.SEND_STATUS_FAILURE);
                        if (messageStatusUpdater != null) {
                            messageStatusUpdater.accept(sendMsg);
                        }
                    });
                }
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                sendMsg.setSendStatus(Message.SEND_STATUS_FAILURE);
                if (messageStatusUpdater != null) {
                    messageStatusUpdater.accept(sendMsg);
                }
            });
        }
    }
}