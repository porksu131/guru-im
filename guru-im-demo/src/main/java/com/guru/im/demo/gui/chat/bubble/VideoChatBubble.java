package com.guru.im.demo.gui.chat.bubble;

import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.gui.component.VideoPreviewDialog;
import com.guru.im.demo.model.Message;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class VideoChatBubble extends AbstractMediaChatBubble {
    private VideoPreviewDialog videoPreviewDialog;

    public VideoChatBubble(ChatPanel chatPanel, Message message, boolean isRight) {
        super(chatPanel, message, isRight);
    }

    @Override
    protected void loadThumbnail() {
        if (isRight) {
            loadFromLocalCache();
        } else {
            loadFromUrl();
        }
    }

    @Override
    protected void showPreviewDialog() {
        if (isRight && !isUploadComplete) {
            return;
        }

        if(videoPreviewDialog == null) {
            Window windowAncestor = SwingUtilities.getWindowAncestor(this);
            videoPreviewDialog = new VideoPreviewDialog((JFrame) windowAncestor);
        }

        if (StringUtils.isNotBlank(originalPath) && new File(originalPath).exists()) {
            videoPreviewDialog.showVideo(fileName, originalPath);
        } else if (StringUtils.isNotBlank(fileSavePath) && new File(fileSavePath).exists()) {
            videoPreviewDialog.showVideo(fileName, fileSavePath);
        } else if (StringUtils.isNotBlank(originalUrl)) {
            videoPreviewDialog.showVideo(fileName, originalUrl);
        }else {
            JOptionPane.showMessageDialog(this, "视频加载错误，无效的视频文件或视频地址: ",
                    "错误", JOptionPane.ERROR_MESSAGE);
        }


        JPanel buttonPanel = new JPanel(new FlowLayout());

        if (isUploadComplete || !isRight) {
            buttonPanel.add(createDownloadBtn(videoPreviewDialog));
        }

        if (hasDownloadTransfer && isDownloadComplete) {
            buttonPanel.add(createOpenFolderBtn("文件所在文件夹", fileSavePath));
        }
        if (isRight && isUploadComplete) {
            buttonPanel.add(createOpenFolderBtn("源文件所在文件夹", originalPath));
        }
        if (hasDownloadTransfer && isDownloadComplete) {
            buttonPanel.add(createOpenFolderBtn("文件所在文件夹", fileSavePath));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (thumbnailImage != null && (showControlButton || showViewOptions)) {
            // 如果显示了控制按钮或查看选项，就不重复绘制视频标识
            return;
        }

        if (isRight) {
            if (!isUploadComplete || (hasDownloadTransfer && !isDownloadComplete)) {
                return;
            }
        } else {
            if ((hasDownloadTransfer && !isDownloadComplete)) {
                return;
            }
        }


        drawVideoIndicator(g);
    }

    /**
     * 绘制视频标识（播放按钮和时长）
     */
    private void drawVideoIndicator(Graphics g) {
        if (thumbnailImage == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int imgX = PADDING;
        int imgY = PADDING;
        int imgWidth = thumbnailImage.getWidth();
        int imgHeight = thumbnailImage.getHeight();


        // 1. 在缩略图中央绘制播放按钮
        int playButtonSize = 40;
        int playButtonX = imgX + (imgWidth - playButtonSize) / 2;
        int playButtonY = imgY + (imgHeight - playButtonSize) / 2;

        // 播放按钮背景（圆形半透明黑色）
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillOval(playButtonX, playButtonY, playButtonSize, playButtonSize);

        // 播放按钮三角形（向右）
        g2d.setColor(Color.WHITE);
        int triangleSize = 20;
        int triangleX = playButtonX + (playButtonSize - triangleSize) / 2 + 4; // 向右偏移4像素更美观
        int triangleY = playButtonY + (playButtonSize - triangleSize) / 2;

        int[] xPoints = {
                triangleX,
                triangleX,
                triangleX + triangleSize - 5
        };
        int[] yPoints = {
                triangleY,
                triangleY + triangleSize,
                triangleY + triangleSize / 2
        };
        g2d.fillPolygon(xPoints, yPoints, 3);

        // 2. 在右下角显示视频时长
        String duration = getVideoDuration(); // 获取视频时长
        if (duration != null && !duration.isEmpty()) {
            g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(duration);
            int textHeight = fm.getHeight();

            // 时长背景（圆角矩形）
            int durationX = imgX + imgWidth - textWidth - 8;
            int durationY = imgY + imgHeight - textHeight - 6;
            int durationWidth = textWidth + 8;
            int durationHeight = textHeight + 2;

            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(durationX - 4, durationY - 2, durationWidth, durationHeight, 8, 8);

            // 时长文字
            g2d.setColor(Color.WHITE);
            g2d.drawString(duration, durationX, durationY + textHeight - 4);
        }

        // 3. 在左上角显示视频图标（可选）
        drawVideoIcon(g2d, imgX, imgY);

        g2d.dispose();
    }

    /**
     * 在左上角绘制视频摄像机图标
     */
    private void drawVideoIcon(Graphics2D g2d, int x, int y) {
        int iconSize = 24;
        int iconX = x + 7;
        int iconY = y + 7;

        // 图标背景（圆形）
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillOval(iconX, iconY, iconSize, iconSize);

        // 摄像机图标
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));

        // 摄像机主体
        g2d.drawRoundRect(iconX + 5, iconY + 7, 10, 8, 2, 2);

        // 镜头
        g2d.fillOval(iconX + 8, iconY + 9, 4, 4);

        // 支架
        g2d.drawLine(iconX + 15, iconY + 9, iconX + 17, iconY + 7);
        g2d.drawLine(iconX + 17, iconY + 7, iconX + 17, iconY + 15);
    }

    /**
     * 获取视频时长显示文本
     */
    private String getVideoDuration() {
        // 这里可以从 MediaInfo 或其他地方获取视频时长
        // 暂时返回一个示例时长，实际应该从消息数据中获取
        Long duration = message.getMediaInfo().getVideoDuration(); // 假设有这个字段
        if (duration != null && duration > 0) {
            return formatDuration(duration);
        }

        // 如果没有时长信息，可以返回空字符串或默认值
        return "00:00"; // 或者返回空字符串 ""
    }

    /**
     * 格式化时长（微秒转换为 HH:MM:SS 格式，始终显示小时）
     */
    private String formatDuration(long durationMicroseconds) {
        if (durationMicroseconds <= 0) {
            return "00:00:00";
        }

        long totalMillisSeconds = durationMicroseconds / 1000;
        long totalSeconds = totalMillisSeconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}