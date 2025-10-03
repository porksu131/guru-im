package com.guru.im.demo.gui.chat;

import com.guru.im.demo.model.Message;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message m1, Message m2) {
        // 优先比较服务端序列号（两者都有有效值）
        boolean m1HasServerSeq = m1.getServerSeq() != null && m1.getServerSeq() >= 1;
        boolean m2HasServerSeq = m2.getServerSeq() != null && m2.getServerSeq() >= 1;

        if (m1HasServerSeq && m2HasServerSeq) {
            return Long.compare(m1.getServerSeq(), m2.getServerSeq());
        }

        // 如果一个有服务端序列号，一个没有，有的一方排在前面
        if (m1HasServerSeq && !m2HasServerSeq) {
            return -1;
        }
        if (!m1HasServerSeq && m2HasServerSeq) {
            return 1;
        }

        // 两者都没有有效服务端序列号，比较客户端序列号
        boolean m1HasClientSeq = m1.getClientSeq() != null && m1.getClientSeq() >= 1;
        boolean m2HasClientSeq = m2.getClientSeq() != null && m2.getClientSeq() >= 1;

        if (m1HasClientSeq && m2HasClientSeq) {
            return Long.compare(m1.getClientSeq(), m2.getClientSeq());
        }

        // 处理其他情况（比如都没有有效序列号）
        if (m1HasClientSeq && !m2HasClientSeq) {
            return -1; // 或者 1
        }
        if (!m1HasClientSeq && m2HasClientSeq) {
            return 1; // 或者 -1
        }

        // 最后按其他字段排序（比如消息时间戳），确保总有确定的顺序
        return Long.compare(m1.getClientSendTime(), m2.getClientSendTime());
    }
}
