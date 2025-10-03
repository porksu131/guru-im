package com.guru.im.demo.gui.chat.bubble;

import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.gui.component.CustomScrollBar;
import com.guru.im.demo.gui.component.ImagePreviewDialog;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

public class ImageChatBubble extends AbstractMediaChatBubble {
    private BufferedImage originalImage;
    private ImagePreviewDialog imagePreviewDialog;

    public ImageChatBubble(ChatPanel chatPanel, Message message, boolean isRight) {
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
        Window windowAncestor = SwingUtilities.getWindowAncestor(this);
        imagePreviewDialog = new ImagePreviewDialog((JFrame) windowAncestor);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        if (isUploadComplete || !isRight) {
            buttonPanel.add(createDownloadBtn(imagePreviewDialog));
        }

        if (!isRight) {
            if (hasDownloadTransfer && isDownloadComplete && new File(message.getDownloadTransfer().getFileSavePath()).exists()) {
                buttonPanel.add(createOpenFolderBtn("文件所在文件夹", message.getDownloadTransfer().getFileSavePath()));
            }
        } else {
            if (isUploadComplete && new File(message.getUploadTransfer().getFilePath()).exists()) {
                buttonPanel.add(createOpenFolderBtn("源文件所在文件夹", message.getUploadTransfer().getFilePath()));
            }
            if (hasDownloadTransfer && isDownloadComplete
                    && new File(message.getDownloadTransfer().getFileSavePath()).exists()) {
                buttonPanel.add(createOpenFolderBtn("文件所在文件夹", message.getDownloadTransfer().getFileSavePath()));
            }
        }

        imagePreviewDialog.showImage(fileName, loadOriginalImage());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (thumbnailImage != null && (showControlButton || showViewOptions)) {
            return;
        }
        
        drawImageIndicator(g);
    }

    /**
     * 绘制图片标识
     */
    private void drawImageIndicator(Graphics g) {
        if (thumbnailImage == null) return;
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int imgX = PADDING;
        int imgY = PADDING;
        int imgWidth = thumbnailImage.getWidth();
        int imgHeight = thumbnailImage.getHeight();

        // 在左上角显示图片图标
        drawImageIcon(g2d, imgX, imgY);

        g2d.dispose();
    }

    /**
     * 在左上角绘制图片图标
     */
    /**
     * 渐变色彩的图片图标
     */
    private void drawImageIcon(Graphics2D g2d, int x, int y) {
        int iconSize = 24;
        int iconX = x + 6;
        int iconY = y + 6;

        // 渐变背景
        GradientPaint gradient = new GradientPaint(
                iconX, iconY, new Color(74, 144, 226, 220),     // 蓝色
                iconX + iconSize, iconY + iconSize, new Color(155, 89, 182, 220) // 紫色
        );
        g2d.setPaint(gradient);
        g2d.fillOval(iconX, iconY, iconSize, iconSize);

        // 白色图标内容
        g2d.setColor(Color.WHITE);

        // 相机图标
        int centerX = iconX + iconSize / 2;
        int centerY = iconY + iconSize / 2;

        // 相机主体
        g2d.fillRoundRect(centerX - 6, centerY - 4, 12, 8, 2, 2);

        // 镜头
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillOval(centerX - 3, centerY - 2, 6, 6);

        // 镜头中心
        g2d.setColor(new Color(100, 100, 100));
        g2d.fillOval(centerX - 2, centerY - 2, 3, 3);

        // 闪光灯
        g2d.setColor(Color.WHITE);
        g2d.fillRect(centerX + 2, centerY - 5, 3, 3);
    }

    private BufferedImage createScaleImage() {
        return ImageUtil.scaleProportionally(loadOriginalImage(), 780, 580);
    }

    private BufferedImage loadOriginalImage() {
        try {
            if (originalImage != null) {
                return originalImage;
            } else if (originalPath != null) {
                return ImageIO.read(new File(originalPath));
            } else if (originalUrl != null) {
                return ImageIO.read(new URL(originalUrl));
            } else {
                return thumbnailImage;
            }
        } catch (Exception e) {
            return thumbnailImage;
        }
    }
}