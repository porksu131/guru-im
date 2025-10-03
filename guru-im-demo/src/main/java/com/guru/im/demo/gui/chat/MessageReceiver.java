package com.guru.im.demo.gui.chat;

import com.guru.im.demo.model.Message;

import javax.swing.*;
import java.util.function.Consumer;

public class MessageReceiver {
    private Consumer<Message> messageConsumer;

    public MessageReceiver() {
        // 空构造函数，通过setter注入
    }

    public void setMessageConsumer(Consumer<Message> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public void handleReceivedMessage(Message msg) {
        if (messageConsumer != null) {
            SwingUtilities.invokeLater(() -> {
                messageConsumer.accept(msg);
            });
        }
    }
}