package com.guru.im.demo.gui.chat.bubble;

import com.guru.im.demo.convert.MessageBuilder;
import com.guru.im.demo.gui.chat.ChatPanel;
import com.guru.im.demo.gui.file.model.FileTransfer;
import com.guru.im.demo.model.MediaInfo;
import com.guru.im.demo.model.Message;
import com.guru.im.demo.util.FileUtil;
import com.guru.im.demo.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public abstract class AbstractMediaChatBubble extends AbstractChatBubble {
    protected final ChatPanel chatPanel;
    protected final Message message;
    protected BufferedImage thumbnailImage;
    protected String thumbnailUrl;
    protected String thumbnailPath;
    protected String originalUrl;
    protected String originalPath;
    protected String fileSavePath;
    protected String fileName;
    protected boolean isRight;
    protected int uploadProgress = 0;
    protected int downloadProgress = 0;
    protected boolean isUploading = false;
    protected boolean isDownloading = false;
    protected boolean isUploadPaused = true;
    protected boolean isDownloadPaused = true;
    protected boolean isUploadComplete = false;
    protected boolean isDownloadComplete = false;
    protected boolean hasDownloadTransfer = false;
    protected boolean showControlButton = false;
    protected boolean showViewOptions = false;

    protected final int ARC_RADIUS = 20;
    protected final int POINTER_SIZE = 10;
    protected final int SHADOW_SIZE = 2;
    protected final int PADDING = 8;
    protected final int THUMBNAIL_MAX_WIDTH = 140;
    protected final int THUMBNAIL_MAX_HEIGHT = 100;
    protected final int PROGRESS_HEIGHT = 6;
    protected final int BUTTON_SIZE = 40;
    protected final int INFO_HEIGHT = 22; // 文件信息区域高度

    protected Color BUBBLE_COLOR;
    protected Color PROGRESS_BG_COLOR;
    protected Color PROGRESS_FG_COLOR;

    public AbstractMediaChatBubble(ChatPanel chatPanel, Message message, boolean isRight) {
        this.chatPanel = chatPanel;
        this.message = message;
        MediaInfo mediaInfo = message.getMediaInfo();
        this.thumbnailUrl = mediaInfo.getThumbnailUrl();
        this.originalUrl = mediaInfo.getFileUrl();
        this.thumbnailPath = mediaInfo.getLocalThumbnailPath();
        this.originalPath = mediaInfo.getLocalFilePath();
        this.fileName = mediaInfo.getFileName();
        this.isRight = isRight;
        this.BUBBLE_COLOR = isRight ? new Color(14, 132, 233) : new Color(230, 230, 230);
        this.PROGRESS_BG_COLOR = new Color(200, 200, 200, 150);
        this.PROGRESS_FG_COLOR = new Color(0, 150, 136);

        setOpaque(false);
        setAlignmentX(isRight ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder());

        initializeTransferStates();
        loadThumbnail();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                updateHoverState();
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
                handleClick();
            }
        });
    }

    protected void initializeTransferStates() {
        if (isRight) {
            FileTransfer fileTransfer = message.getUploadTransfer();
            if (fileTransfer != null) {
                this.uploadProgress = fileTransfer.getProgress();
                this.isUploadComplete = (uploadProgress == 100);
                this.isUploading = (uploadProgress < 100 && !isUploadPaused);
            } else {
                this.isUploadComplete = true;
            }
        }

        FileTransfer downloadTransfer = message.getDownloadTransfer();
        if (downloadTransfer != null) {
            this.hasDownloadTransfer = true;
            this.downloadProgress = downloadTransfer.getProgress();
            this.isDownloadComplete = (downloadProgress == 100);
            this.isDownloading = (downloadProgress < 100 && !isDownloadPaused);
            this.fileSavePath = downloadTransfer.getFileSavePath();
        }
    }

    protected void updateHoverState() {
        if (isRight) {
            if (!isUploadComplete) {
                showControlButton = true;
                showViewOptions = false;
            } else if (hasDownloadTransfer && !isDownloadComplete) {
                showControlButton = true;
                showViewOptions = false;
            } else if (isUploadComplete) {
                showControlButton = false;
                showViewOptions = true;
            }
        } else {
            if (hasDownloadTransfer && !isDownloadComplete) {
                showControlButton = true;
                showViewOptions = false;
            } else {
                showControlButton = false;
                showViewOptions = true;
            }
        }
    }

    protected void handleClick() {
        if (isRight) {
            if (!isUploadComplete) {
                toggleTransfer();
            } else if (hasDownloadTransfer && !isDownloadComplete) {
                toggleTransfer();
            } else if (isUploadComplete) {
                showPreviewDialog();
            }
        } else {
            if (hasDownloadTransfer && !isDownloadComplete) {
                toggleTransfer();
            } else {
                showPreviewDialog();
            }
        }
    }

    protected void toggleTransfer() {
        if (isRight && !isUploadComplete) {
            if (isUploadPaused) {
                resumeUpload();
            } else {
                pauseUpload();
            }
        } else if (hasDownloadTransfer && !isDownloadComplete) {
            if (isDownloadPaused) {
                resumeDownload();
            } else {
                pauseDownload();
            }
        }
        repaint();
    }

    protected abstract void loadThumbnail();

    protected abstract void showPreviewDialog();

    public void reloadLocalThumbnail() {
        this.thumbnailPath = message.getUploadTransfer().getThumbnailPath();
        try {
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                File thumbnailFile = new File(thumbnailPath);
                if (thumbnailFile.exists()) {
                    thumbnailImage = createScaleThumbnail(ImageIO.read(thumbnailFile));
                    SwingUtilities.invokeLater(() -> {
                        revalidate();
                        repaint();
                    });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFromLocalCache() {
        if (thumbnailImage == null) {
            BufferedImage bufferedImage = loadCachedThumbnail();
            if (bufferedImage != null) {
                thumbnailImage = createScaleThumbnail(bufferedImage);
            }
        }

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    protected BufferedImage loadCachedThumbnail() {
        try {
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                File cachedFile = new File(thumbnailPath);
                if (cachedFile.exists()) {
                    return ImageIO.read(cachedFile);
                } else if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    return getThumbnailImage(thumbnailUrl);
                }
            }

            return ImageUtil.loadResourceImage("image/image-placeholder.png");
        } catch (Exception e) {
            System.err.println("Failed to load from cache: " + e.getMessage());
        }
        return null;
    }

    protected void loadFromUrl() {
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    thumbnailImage = createScaleThumbnail(getThumbnailImage(thumbnailUrl));
                    SwingUtilities.invokeLater(() -> {
                        revalidate();
                        repaint();
                    });
                } catch (IOException e) {
                    System.err.println("Failed to load thumbnail from URL: " + e.getMessage());
                }
            }).start();
        }
    }

    private BufferedImage getThumbnailImage(String thumbnailUrl) throws IOException {
        try {
            URL url = new URL(thumbnailUrl);
            return ImageIO.read(url);
        } catch (IOException e) {
            return ImageUtil.loadResourceImage("image/image-placeholder.png");
        }
    }

    protected BufferedImage createScaleThumbnail(BufferedImage sourceImage) {
        if (sourceImage == null) return null;
        return ImageUtil.scaleProportionally(sourceImage, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
    }

    protected JButton createOpenFolderBtn(String text, String filePath) {
        JButton openFolderBtn = new JButton(text);
        openFolderBtn.addActionListener(e -> FileUtil.openFolderAndSelectFile(filePath));
        return openFolderBtn;
    }

    protected JButton createDownloadBtn(JFrame dialog) {
        JButton downloadBtn = new JButton("下载原文件");
        downloadBtn.addActionListener(e -> downloadFile(dialog));
        return downloadBtn;
    }

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
            if (parent instanceof Dialog) {
                ((Dialog) parent).dispose();
            }

            startDownload(fileToSave);
        }
    }

    protected void startDownload(File targetFile) {
        isDownloading = true;
        isDownloadPaused = false;
        isDownloadComplete = false;
        downloadProgress = 0;

        FileTransfer fileTransfer = getOrCreateDownloadTransfer(targetFile);
        this.hasDownloadTransfer = true;
        this.fileSavePath = targetFile.getAbsolutePath();
        message.setDownloadTransfer(fileTransfer);

        SwingUtilities.invokeLater(this::repaint);

        new Thread(() -> {
            try {
                message.setSendStatus(Message.SEND_STATUS_DOWNLOADING);
                this.chatPanel.startDownload(fileTransfer);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                            "下载失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
                isDownloading = false;
                isDownloadPaused = true;
            }
        }).start();
    }

    protected FileTransfer getOrCreateDownloadTransfer(File targetFile) {
        if (hasDownloadTransfer) {
            FileTransfer downloadTransfer = message.getDownloadTransfer();
            if (downloadTransfer != null) {
                downloadTransfer.setStatus(FileTransfer.Status.PENDING);
                downloadTransfer.setProgress(downloadProgress);
                downloadTransfer.setPaused(isDownloadPaused);
                downloadTransfer.setCancelled(false);
                downloadTransfer.setFileSavePath(targetFile.getAbsolutePath());
                return downloadTransfer;
            }
        }
        FileTransfer fileTransfer = MessageBuilder.buildFileDownloadTransfer(message);
        fileTransfer.setStatus(FileTransfer.Status.PENDING);
        fileTransfer.setProgress(downloadProgress);
        fileTransfer.setPaused(isDownloadPaused);
        fileTransfer.setFileSavePath(targetFile.getAbsolutePath());
        return fileTransfer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int width = getWidth() - SHADOW_SIZE;
        int height = getHeight() - SHADOW_SIZE;

        g2d.setColor(new Color(0, 0, 0, 25));
        g2d.fillRoundRect(SHADOW_SIZE, SHADOW_SIZE, width, height, ARC_RADIUS, ARC_RADIUS);

        RoundRectangle2D.Double bubble = new RoundRectangle2D.Double(0, 0, width, height, ARC_RADIUS, ARC_RADIUS);
        g2d.setColor(BUBBLE_COLOR);
        g2d.fill(bubble);

        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.draw(new RoundRectangle2D.Double(0.5, 0.5, width - 1, height - 1, ARC_RADIUS, ARC_RADIUS));

        if (thumbnailImage != null) {
            int imgX = PADDING;
            int imgY = PADDING;
            int imgWidth = thumbnailImage.getWidth();
            int imgHeight = thumbnailImage.getHeight();

            // 绘制缩略图
            g2d.drawImage(thumbnailImage, imgX, imgY, imgWidth, imgHeight, null);

            // 绘制文件信息区域（半透明背景）
            int infoY = imgY + imgHeight;
            int infoHeight = INFO_HEIGHT;

            // 文件信息背景
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRoundRect(imgX, infoY, imgWidth, infoHeight, 0, 0);

            // 文件信息文字
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
            FontMetrics fm = g2d.getFontMetrics();

            // 文件大小
            String fileSize = FileUtil.formatFileSize(message.getMediaInfo().getFileSize());
            int fileSizeWidth = fm.stringWidth(fileSize);

            // 文件名
            String displayFileName = FileUtil.truncateFileName(fileName, fm, imgWidth - fileSizeWidth - 15);
            int displayFileNameWidth = fm.stringWidth(displayFileName);

            g2d.drawString(displayFileName, imgX + 5, infoY + 15);
            g2d.drawString(fileSize, imgX + 5 + displayFileNameWidth + 5, infoY + 15);

            int currentProgress = 0;
            boolean showProgress = false;

            if (hasDownloadTransfer && !isDownloadComplete) {
                currentProgress = downloadProgress;
                showProgress = true;
            } else if (isRight && !isUploadComplete) {
                currentProgress = uploadProgress;
                showProgress = true;
            }

            if (showProgress && currentProgress < 100) {
                int circleDiameter = 50;
                int circleX = imgX + (imgWidth - circleDiameter) / 2;
                int circleY = imgY + (imgHeight - circleDiameter) / 2;

                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillOval(circleX, circleY, circleDiameter, circleDiameter);

                g2d.setColor(PROGRESS_FG_COLOR);
                g2d.setStroke(new BasicStroke(3));
                int startAngle = 90;
                int arcAngle = (int) (-360 * currentProgress / 100.0);
                g2d.drawArc(circleX + 3, circleY + 3, circleDiameter - 6, circleDiameter - 6, startAngle, arcAngle);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
                String progressText = currentProgress + "%";
                int textWidth = fm.stringWidth(progressText);
                int textHeight = fm.getHeight();
                g2d.drawString(progressText,
                        circleX + (circleDiameter - textWidth) / 2,
                        circleY + (circleDiameter + textHeight) / 2 - 2);
            }

            // 绘制遮罩层（当显示按钮时）
            if (showControlButton || showViewOptions) {
                // 半透明黑色遮罩
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRoundRect(imgX, imgY, imgWidth, imgHeight + infoHeight, 0, 0);

                // 按钮位置：基于遮罩层居中（imgX, imgY, imgWidth, imgHeight + infoHeight）
                int maskWidth = imgWidth;
                int maskHeight = imgHeight + infoHeight;
                int btnX = imgX + (maskWidth - BUTTON_SIZE) / 2;
                int btnY = imgY + (maskHeight - BUTTON_SIZE) / 2;

                // 按钮背景
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(btnX, btnY, BUTTON_SIZE, BUTTON_SIZE, 20, 20);

                // 按钮图标
                g2d.setColor(new Color(0, 0, 0, 180));
                if (showControlButton) {
                    drawControlButtonIcon(g2d, btnX, btnY);
                } else if (showViewOptions) {
                    // 查看图标（放大镜）基于按钮区域居中绘制
                    int iconX = btnX + BUTTON_SIZE / 2 - 10; // 居中偏移
                    int iconY = btnY + BUTTON_SIZE / 2 - 10;
                    int iconSize = 24;
                    ImageUtil.setHighQualityRenderingHints(g2d);
                    BufferedImage searchIcon = ImageUtil.loadSvgImage("image/search.svg", iconSize, null);
                    g2d.drawImage(searchIcon, iconX, iconY, iconSize, iconSize, null);
                }
            }
        }

        g2d.dispose();
    }

    protected void drawControlButtonIcon(Graphics2D g2d, int x, int y) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean isPaused = false;
        if (isRight && !isUploadComplete) {
            isPaused = isUploadPaused;
        } else if (hasDownloadTransfer && !isDownloadComplete) {
            isPaused = isDownloadPaused;
        }

        if (isPaused) {
            // 播放图标（向右三角形）
            int centerX = x + BUTTON_SIZE / 2;
            int centerY = y + BUTTON_SIZE / 2;
            int triangleSize = 16;

            int[] xPoints = {centerX - triangleSize / 3, centerX - triangleSize / 3, centerX + triangleSize / 2};
            int[] yPoints = {centerY - triangleSize / 2, centerY + triangleSize / 2, centerY};
            g2d.fillPolygon(xPoints, yPoints, 3);
        } else {
            // 暂停图标（两条竖线）
            int centerX = x + BUTTON_SIZE / 2;
            int centerY = y + BUTTON_SIZE / 2;
            int lineWidth = 4;
            int lineHeight = 20;
            int gap = 6;

            g2d.fillRect(centerX - gap / 2 - lineWidth, centerY - lineHeight / 2, lineWidth, lineHeight);
            g2d.fillRect(centerX + gap / 2, centerY - lineHeight / 2, lineWidth, lineHeight);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (thumbnailImage == null) {
            return new Dimension(THUMBNAIL_MAX_WIDTH + PADDING * 2,
                    THUMBNAIL_MAX_HEIGHT + INFO_HEIGHT + PADDING * 2 + 2);
        }

        int width = thumbnailImage.getWidth() + PADDING * 2;
        int height = thumbnailImage.getHeight() + INFO_HEIGHT + PADDING * 2 + 2;
        return new Dimension(width, height);
    }

    public void setUploadProgress(int progress) {
        this.uploadProgress = Math.max(0, Math.min(100, progress));
        this.isUploading = progress < 100;
        this.isUploadComplete = (progress == 100);
        this.isUploadPaused = false;
        this.showControlButton = false;
        repaint();
    }

    public void setDownloadProgress(int progress) {
        this.downloadProgress = Math.max(0, Math.min(100, progress));
        this.isDownloading = progress < 100;
        this.isDownloadComplete = (progress == 100);
        this.isDownloadPaused = false;
        repaint();
    }

    public void uploadFailed() {
        if (isUploading) {
            isUploadPaused = true;
            isUploading = false;
            repaint();
        }
    }

    public void downloadFailed() {
        if (isDownloading) {
            isDownloadPaused = true;
            isDownloading = false;
            repaint();
        }
    }

    protected void pauseUpload() {
        if (isUploading) {
            isUploadPaused = true;
            isUploading = false;
            repaint();
            chatPanel.pauseUpload(this.message);
        }
    }

    protected void resumeUpload() {
        if (isUploadPaused) {
            isUploadPaused = false;
            isUploading = true;
            repaint();
            chatPanel.resumeUpload(this.message);
        }
    }

    protected void pauseDownload() {
        if (isDownloading) {
            isDownloadPaused = true;
            isDownloading = false;
            repaint();
            chatPanel.pauseDownload(this.message);
        }
    }

    protected void resumeDownload() {
        if (isDownloadPaused) {
            isDownloadPaused = false;
            isDownloading = true;
            repaint();
            chatPanel.resumeDownload(this.message);
        }
    }

    public void completeDownload() {
        downloadProgress = 100;
        isDownloading = false;
        isDownloadComplete = true;
        repaint();
    }
}