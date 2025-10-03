package com.guru.im.demo.gui.chat.bubble;

import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.util.FileUtil;
import com.guru.im.demo.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class FileChatBubble extends AbstractMediaChatBubble {
    private static final int ICON_SIZE = 32;
    private static final int BUBBLE_WIDTH = 250;
    private static final int BUBBLE_HEIGHT = 60;
    private static final int TEXT_MAX_WIDTH = 150;
    private static final int FILE_SIZE_WIDTH = 50;

    public FileChatBubble(ChatPanel chatPanel, Message message, boolean isRight) {
        super(chatPanel, message, isRight);
        // 移除原有的鼠标监听器，添加新的下载功能监听器
        removeAllMouseListeners();
        addMouseListener(new FileMouseAdapter());
    }

    private void removeAllMouseListeners() {
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }

    @Override
    protected void loadThumbnail() {
        // 文件气泡不需要加载缩略图，使用静态图标
    }

    @Override
    protected void showPreviewDialog() {
        // 文件气泡不再使用预览对话框，改为直接下载
        // 这个方法可以留空或者抛出异常，因为不会被调用
    }

    private class FileMouseAdapter extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            updateFileHoverState();
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            showControlButton = false;
            showViewOptions = false;
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            handleFileClick();
        }
    }

    private void updateFileHoverState() {
        if (isRight) {
            // 右侧消息：显示上传状态控制按钮
            if (!isUploadComplete) {
                showControlButton = true;
                showViewOptions = false;
            } else if (hasDownloadTransfer && !isDownloadComplete) {
                showControlButton = true;
                showViewOptions = false;
            } else {
                // 上传完成，显示打开文件夹选项
                showControlButton = false;
                showViewOptions = true;
            }
        } else {
            // 左侧消息：显示下载状态控制按钮
            if (hasDownloadTransfer && !isDownloadComplete) {
                showControlButton = true;
                showViewOptions = false;
            } else {
                // 未下载或下载完成，显示下载/打开选项
                showControlButton = false;
                showViewOptions = true;
            }
        }
    }

    private void handleFileClick() {
        if (isRight) {
            // 右侧消息逻辑
            if (!isUploadComplete) {
                // 上传未完成：暂停/继续上传
                toggleTransfer();
            } else if (hasDownloadTransfer && !isDownloadComplete) {
                // 上传完成但下载未完成：暂停/继续下载
                toggleTransfer();
            } else {
                // 上传完成：打开源文件所在文件夹
                openSourceFileFolder();
            }
        } else {
            // 左侧消息逻辑
            if (hasDownloadTransfer && !isDownloadComplete) {
                // 下载未完成：暂停/继续下载
                toggleTransfer();
            } else if (hasDownloadTransfer && isDownloadComplete && new File(message.getDownloadTransfer().getFileSavePath()).exists()) {
                // 下载完成：打开下载文件所在文件夹
                openDownloadedFileFolder();
            } else {
                // 未下载：开始下载
                startFileDownload();
            }
        }
    }

    private void openSourceFileFolder() {
        if (originalPath != null) {
            FileUtil.openFolderAndSelectFile(originalPath);
        }
    }

    private void openDownloadedFileFolder() {
        if (hasDownloadTransfer && isDownloadComplete) {
            String filePath = message.getDownloadTransfer().getFileSavePath();
            if (filePath != null) {
                FileUtil.openFolderAndSelectFile(filePath);
            }
        }
    }

    private void startFileDownload() {
        Window windowAncestor = SwingUtilities.getWindowAncestor(this);
        downloadFile(windowAncestor);
    }

    @Override
    protected void downloadFile(Component parent) {
        if (isRight && !isUploadComplete) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存文件");
        fileChooser.setSelectedFile(new File(fileName != null && !fileName.isEmpty() ? fileName : "file"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            startDownload(fileToSave);
        }
    }

    private String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "file";
    }

    private BufferedImage getFileIcon() {
        String extension = getFileExtension();
        BufferedImage icon = ImageUtil.FILE_ICON_CACHE.get(extension);
        if (icon == null) {
            icon = ImageUtil.FILE_ICON_CACHE.get("file"); // 使用默认文件图标
        }
        return icon;
    }

    @Override
    public Dimension getPreferredSize() {
        int height = BUBBLE_HEIGHT;
        if (shouldShowProgress()) {
            height += 5; // 为进度条增加额外高度
        }
        return new Dimension(BUBBLE_WIDTH, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth() - SHADOW_SIZE;
        int height = getHeight() - SHADOW_SIZE;

        // 绘制阴影
        g2d.setColor(new Color(0, 0, 0, 25));
        g2d.fillRoundRect(SHADOW_SIZE, SHADOW_SIZE, width, height, ARC_RADIUS, ARC_RADIUS);

        // 绘制气泡
        RoundRectangle2D.Double bubble = new RoundRectangle2D.Double(0, 0, width, height, ARC_RADIUS, ARC_RADIUS);
        g2d.setColor(BUBBLE_COLOR);
        g2d.fill(bubble);

        // 绘制高光效果
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.draw(new RoundRectangle2D.Double(0.5, 0.5, width - 1, height - 1, ARC_RADIUS, ARC_RADIUS));

        // 绘制文件图标
        BufferedImage fileIcon = getFileIcon();
        if (fileIcon != null) {
            int iconX = PADDING;
            int iconY = (getHeight() - ICON_SIZE) / 2;
            g2d.drawImage(fileIcon, iconX, iconY, ICON_SIZE, ICON_SIZE, null);
        }

        // 计算文本区域
        int textAreaX = PADDING + ICON_SIZE + 8;
        int textAreaWidth = width - textAreaX - PADDING;

        // 检查是否显示进度条
        boolean showProgress = shouldShowProgress();

        // 绘制文件名
        if (fileName != null) {
            g2d.setColor(isRight ? Color.WHITE : Color.BLACK);
            g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

            FontMetrics fm = g2d.getFontMetrics();

            // 根据是否显示进度条调整文件名区域高度
            int fileNameMaxWidth = textAreaWidth;
            int fileNameY;

            if (showProgress) {
                // 显示进度条时，文件名在上方
                fileNameY = (getHeight() - fm.getHeight() - 13) / 2 + fm.getAscent();
            } else {
                // 不显示进度条时，文件名居中
                fileNameY = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            }

            String displayName = FileUtil.truncateFileName(fileName, fm, fileNameMaxWidth);
            g2d.drawString(displayName, textAreaX, fileNameY);
        }

        // 绘制文件大小（在底部右侧）
        if (message.getMediaInfo() != null && message.getMediaInfo().getFileSize() > 0) {
            String fileSize = FileUtil.formatFileSize(message.getMediaInfo().getFileSize());
            g2d.setColor(isRight ? new Color(255, 255, 255, 180) : new Color(0, 0, 0, 120));
            g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));

            FontMetrics fm = g2d.getFontMetrics();
            int sizeX = getWidth() - PADDING - fm.stringWidth(fileSize) - 8;

            // 文件大小位置根据是否显示进度条调整
            int sizeY;
            if (showProgress) {
                sizeY = getHeight() - PADDING - 13; // 为进度条留出空间
            } else {
                sizeY = getHeight() - PADDING;
            }

            g2d.drawString(fileSize, sizeX, sizeY);
        }

        // 绘制进度条和控制按钮
        if (showProgress) {
            drawProgressBar(g2d, textAreaX, textAreaWidth);
        }

        // 绘制遮罩层和操作按钮（鼠标滑过时显示）
        if (showControlButton || showViewOptions) {
            drawHoverOverlay(g2d);
        }

        g2d.dispose();
    }

    /**
     * 检查是否应该显示进度条
     */
    private boolean shouldShowProgress() {
        if (hasDownloadTransfer && !isDownloadComplete) {
            return true;
        } else if (isRight && !isUploadComplete) {
            return true;
        }
        return false;
    }

    /**
     * 绘制进度条（单独方法，简化逻辑）
     */
    private void drawProgressBar(Graphics2D g2d, int startX, int availableWidth) {
        int currentProgress = 0;

        if (hasDownloadTransfer && !isDownloadComplete) {
            currentProgress = downloadProgress;
        } else if (isRight && !isUploadComplete) {
            currentProgress = uploadProgress;
        }

        if (currentProgress < 100) {
            int progressBarWidth = Math.max(50, availableWidth - 40); // 为百分比文本留出空间
            int progressBarX = startX;
            int progressBarY = getHeight() - PADDING - 2; // 进度条在底部

            // 绘制进度条背景
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(progressBarX, progressBarY, progressBarWidth, 4, 2, 2);

            // 绘制进度条前景
            g2d.setColor(PROGRESS_FG_COLOR);
            int progressWidth = (int) (progressBarWidth * currentProgress / 100.0);
            g2d.fillRoundRect(progressBarX, progressBarY, progressWidth, 4, 2, 2);

            // 绘制进度文本
            g2d.setColor(isRight ? Color.WHITE : Color.BLACK);
            g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 10));
            String progressText = currentProgress + "%";

            FontMetrics fm = g2d.getFontMetrics();
            int textX = startX + progressBarWidth + 6;
            int textY = progressBarY + 4;

            g2d.drawString(progressText, textX, textY);
        }
    }

    /**
     * 绘制鼠标滑过时的遮罩层和操作按钮
     */
    private void drawHoverOverlay(Graphics2D g2d) {
        int width = getWidth() - SHADOW_SIZE;
        int height = getHeight() - SHADOW_SIZE;

        // 绘制半透明遮罩层
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(0, 0, width, height, ARC_RADIUS, ARC_RADIUS);

        // 绘制操作按钮背景
        int btnSize = 36;
        int btnX = (getWidth() - btnSize) / 2;
        int btnY = (getHeight() - btnSize) / 2;

        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(btnX, btnY, btnSize, btnSize, 8, 8);

        // 绘制按钮边框
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(btnX, btnY, btnSize, btnSize, 8, 8);

        // 绘制操作图标
        int iconSize = 20;
        int iconX = btnX + (btnSize - iconSize) / 2;
        int iconY = btnY + (btnSize - iconSize) / 2;

        g2d.setColor(new Color(0, 0, 0, 200));

        if (showControlButton) {
            // 绘制控制按钮（播放/暂停）
            drawControlButtonIcon(g2d, iconX, iconY, iconSize);
        } else if (showViewOptions) {
            // 绘制操作图标（下载或文件夹）
            drawActionIcon(g2d, iconX, iconY, iconSize);
        }
    }

    /**
     * 绘制控制按钮图标（播放/暂停）
     */
    private void drawControlButtonIcon(Graphics2D g2d, int x, int y, int size) {
        boolean isPaused = false;
        if (isRight && !isUploadComplete) {
            isPaused = isUploadPaused;
        } else if (hasDownloadTransfer && !isDownloadComplete) {
            isPaused = isDownloadPaused;
        }

        if (isPaused) {
            // 播放图标（向右三角形）
            int triangleSize = size - 6;
            int centerX = x + size / 2;
            int centerY = y + size / 2;

            int[] xPoints = {
                    centerX - triangleSize / 3,
                    centerX - triangleSize / 3,
                    centerX + triangleSize / 2
            };
            int[] yPoints = {
                    centerY - triangleSize / 2,
                    centerY + triangleSize / 2,
                    centerY
            };
            g2d.fillPolygon(xPoints, yPoints, 3);
        } else {
            // 暂停图标（两条竖线）
            int centerX = x + size / 2;
            int centerY = y + size / 2;
            int lineWidth = 3;
            int lineHeight = size - 8;
            int gap = 4;

            g2d.fillRect(centerX - gap / 2 - lineWidth, centerY - lineHeight / 2, lineWidth, lineHeight);
            g2d.fillRect(centerX + gap / 2, centerY - lineHeight / 2, lineWidth, lineHeight);
        }
    }

    /**
     * 绘制操作图标（下载或文件夹）
     */
    private void drawActionIcon(Graphics2D g2d, int iconX, int iconY, int iconSize) {
        if (isRight) {
            // 右侧消息：文件夹图标（打开源文件）
            drawIcon(g2d, "image/file/folder.svg", iconX, iconY, iconSize);
        } else {
            if (hasDownloadTransfer && isDownloadComplete && new File(message.getDownloadTransfer().getFileSavePath()).exists()) {
                // 左侧消息已下载：文件夹图标（打开下载文件）
                drawIcon(g2d, "image/file/folder.svg", iconX, iconY, iconSize);
            } else {
                // 左侧消息未下载：下载图标
                drawIcon(g2d, "image/file/download.svg", iconX, iconY, iconSize);
            }
        }
    }

    private void drawIcon(Graphics2D g2d, String resourcePatch, int x, int y, int size) {
        ImageIcon svgIcon = ImageUtil.createSVGIcon(resourcePatch, size);
        BufferedImage bufferedImage = ImageUtil.convertSVGIconToBufferedImage(svgIcon);
        ImageUtil.setHighQualityRenderingHints(g2d);
        g2d.drawImage(bufferedImage, x, y, size, size, null);
    }

}