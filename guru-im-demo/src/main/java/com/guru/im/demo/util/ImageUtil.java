package com.guru.im.demo.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.guru.im.demo.gui.file.model.GrabberVideoInfo;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.nutz.img.Images;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageUtil {
    public static final Map<String, BufferedImage> FILE_ICON_CACHE = new HashMap<>();

    static {
        // 预加载常用文件类型图标
        loadFileIcons();
    }

    /**
     * 生成图片缩略图
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @param width      缩略图宽度
     * @param height     缩略图高度
     * @return 生成是否成功
     */
    public static boolean generateImageThumbnail(String sourcePath, String targetPath,
                                                 int width, int height) {
        try {
            Thumbnails.of(sourcePath)
                    .size(width, height)
                    .outputFormat("jpg")
                    .toFile(targetPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 生成保持比例的缩略图（裁剪中间部分）
     */
    public static boolean generateImageCroppedThumbnail(String sourcePath, String targetPath,
                                                        int width, int height) {
        try {
            Thumbnails.of(sourcePath)
                    .sourceRegion(Positions.CENTER, width, height)
                    .size(width, height)
                    .outputFormat("jpg")
                    .toFile(targetPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 生成视频缩略图（获取中间帧）
     *
     * @param videoThumbnail 视频文件缩略图
     * @param thumbnailPath  缩略图保存路径
     * @param width          缩略图宽度
     * @param height         缩略图高度
     * @return 生成是否成功
     */
    public static boolean generateVideoThumbnail(BufferedImage videoThumbnail, String thumbnailPath,
                                                 int width, int height) {
        try {
            if (videoThumbnail == null) {
                return false;
            }

            // 调整大小并保存
            BufferedImage resizedImage = resizeVideoImage(videoThumbnail, width, height);
            ImageIO.write(resizedImage, "jpg", new File(thumbnailPath));
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static GrabberVideoInfo generateVideoImage(String videoPath) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();

            // 获取视频总帧数
            int totalFrames = grabber.getLengthInFrames();

            // 跳到中间帧（大约50%位置）
            int middleFrame = totalFrames / 2;
            if (middleFrame > 0) {
                grabber.setFrameNumber(middleFrame);
            }

            // 获取帧并转换为图片
            Frame frame = grabber.grabImage();
            Java2DFrameConverter converter = new Java2DFrameConverter();

            GrabberVideoInfo videoInfo = new GrabberVideoInfo();
            videoInfo.setImageWidth(grabber.getImageWidth());
            videoInfo.setImageHeight(grabber.getImageHeight());
            videoInfo.setVideoBitrate(grabber.getVideoBitrate());
            videoInfo.setVideoCodec(grabber.getVideoCodec());
            videoInfo.setAudioCodec(grabber.getAudioCodec());
            videoInfo.setAudioBitrate(grabber.getAudioBitrate());
            videoInfo.setDuration(grabber.getLengthInTime());
            videoInfo.setScreenshot(converter.getBufferedImage(frame));

            return videoInfo;
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage resizeVideoImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    public static ImageIcon createThumbnail(String imageUrl, int maxWidth, int maxHeight) {
        try {
            URL url = new URL(imageUrl);
            BufferedImage originalImage = ImageIO.read(url);

            if (originalImage != null) {
                // 计算缩略图尺寸
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                double ratio = Math.min((double) maxWidth / originalWidth,
                        (double) maxHeight / originalHeight);

                int thumbWidth = (int) (originalWidth * ratio);
                int thumbHeight = (int) (originalHeight * ratio);

                // 创建缩略图
                Image scaledImage = originalImage.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        URL url = ButtonUtil.class.getClassLoader().getResource("image/image-placeholder.png");
        // 加载失败时返回占位符
        assert url != null;
        return new ImageIcon(url);
    }

    public static BufferedImage loadResourceImage(String resourcePatch) throws IOException {
        URL url = ButtonUtil.class.getClassLoader().getResource(resourcePatch);
        if (url != null) {
            return ImageIO.read(url);
        }
        return null;
    }

    public static Dimension getImageDimension(String imageFilePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(imageFilePath));
            return new Dimension(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return null;
        }
    }


    /**
     * 按最大尺寸限制缩放（保持宽高比）
     *
     * @param originalImage 原始图片
     * @param maxWidth      最大宽度
     * @param maxHeight     最大高度
     * @return 缩放后的图片
     */
    public static BufferedImage scaleProportionally(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算缩放比例
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        // 计算新尺寸
        int scaledWidth = (int) (originalWidth * ratio);
        int scaledHeight = (int) (originalHeight * ratio);

        // 创建缩放后的图片
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // 设置高质量渲染
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaledImage;
    }

    /**
     * 按指定比例缩放
     *
     * @param originalImage 原始图片
     * @param scale         缩放比例（0.0-1.0为缩小，>1.0为放大）
     * @return 缩放后的图片
     */
    public static BufferedImage scaleByPercentage(BufferedImage originalImage, double scale) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return scaledImage;
    }

    /**
     * 设置超高质量渲染参数
     */
    public static void setHighQualityRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static BufferedImage loadSvgImage(String resourcePath, int size, Color color) {
        ImageIcon svgIcon = createSVGIcon(resourcePath, size, color);
        return ImageUtil.convertSVGIconToBufferedImage(svgIcon);
    }

    public static ImageIcon createSVGIcon(String path, int size) {
        URL url = ButtonUtil.class.getClassLoader().getResource(path);
        assert url != null;
        FlatSVGIcon flatSVGIcon = new FlatSVGIcon(url);
        return flatSVGIcon.derive(size, size);
    }

    public static ImageIcon createSVGIcon(String path, int size, Color newColor) {
        URL url = ButtonUtil.class.getClassLoader().getResource(path);
        assert url != null;
        FlatSVGIcon flatSVGIcon = new FlatSVGIcon(url);
        if (newColor != null) {
            flatSVGIcon.setColorFilter(new FlatSVGIcon.ColorFilter(originalColor -> newColor));
        }
        return flatSVGIcon.derive(size, size);
    }

    public static BufferedImage convertSVGIconToBufferedImage(ImageIcon svgIcon) {
        // 获取SVG图标的尺寸
        int width = svgIcon.getIconWidth();
        int height = svgIcon.getIconHeight();

        // 创建BufferedImage（使用ARGB支持透明度）
        BufferedImage bufferedImage = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB
        );

        // 获取Graphics2D并绘制SVG图标
        Graphics2D g2d = bufferedImage.createGraphics();

        // 设置渲染提示以获得更好的质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 绘制SVG图标
        svgIcon.paintIcon(null, g2d, 0, 0);

        g2d.dispose();

        return bufferedImage;
    }


    public static void loadFileIcons() {
        try {
            // 加载各种文件类型图标（这里需要替换为实际的图标资源路径）
            loadIcon("pdf", "image/file/pdf.svg");
            loadIcon("doc", "image/file/doc.svg");
            loadIcon("docx", "image/file/doc.svg");
            loadIcon("xls", "image/file/xls.svg");
            loadIcon("xlsx", "image/file/xls.svg");
            loadIcon("ppt", "image/file/ppt.svg");
            loadIcon("pptx", "image/file/ppt.svg");
            loadIcon("txt", "image/file/txt.svg");
            loadIcon("zip", "image/file/zip.svg");
            loadIcon("rar", "image/file/zip.svg");
            loadIcon("jpg", "image/file/image.svg");
            loadIcon("jpeg", "image/file/image.svg");
            loadIcon("png", "image/file/image.svg");
            loadIcon("gif", "image/file/image.svg");
            loadIcon("mp4", "image/file/video.svg");
            loadIcon("avi", "image/file/video.svg");
            loadIcon("mov", "image/file/video.svg");
            loadIcon("mp3", "image/file/music.svg");
            loadIcon("wav", "image/file/audio.svg");
            loadIcon("file", "image/file/file-default.svg"); // 默认文件图标
        } catch (Exception e) {
            System.err.println("Failed to load file icons: " + e.getMessage());
        }
    }

    public static void loadIcon(String extension, String resourcePath) {
        ImageIcon svgIcon = ImageUtil.createSVGIcon(resourcePath, 32, new Color(182, 180, 180));
        FILE_ICON_CACHE.put(extension, ImageUtil.convertSVGIconToBufferedImage(svgIcon));
    }

    public static Icon getIconForName(String iconText, int size) {
        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(size, size));
        icon.setOpaque(true);
        icon.setHorizontalAlignment(JLabel.CENTER);
        icon.setForeground(Color.WHITE); // 白色文字
        icon.setFont(new Font("微软雅黑", Font.BOLD, calculateOptimalFontSize(size))); // 计算最佳字体大小
        icon.setBackground(generateColorFromName(iconText)); // 基于名称生成颜色
        icon.setText(getDisplayText(iconText)); // 只显示第一个字符

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(icon.getBackground());
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(icon.getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(icon.getText());
                int textHeight = fm.getHeight();
                g2.drawString(icon.getText(), x + (size / 2) - textWidth / 2, y + (size / 2) + textHeight / 4);
            }

            @Override
            public int getIconWidth() {
                return 32;
            }

            @Override
            public int getIconHeight() {
                return 32;
            }
        };
    }

    /**
     * 获取显示文本（1-2个字符）
     */
    private static String getDisplayText(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String trimmed = name.trim();
        if (trimmed.equals("群聊")) {
            return trimmed;
        }
        return trimmed.length() >= 2 ? trimmed.substring(0, 1) : trimmed;
    }

    /**
     * 基于名称生成颜色
     */
    private static Color generateColorFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new Color(0, 120, 215); // 好看的默认蓝色
        }

        int hash = name.hashCode();
        // 使用HSL颜色空间生成更美观的颜色
        float hue = (Math.abs(hash) % 360) / 360.0f;
        float saturation = 0.7f + (Math.abs(hash >> 8) % 30) / 100.0f; // 70-100%
        float lightness = 0.5f + (Math.abs(hash >> 16) % 10) / 100.0f; // 50-60%

        return Color.getHSBColor(hue, Math.min(saturation, 0.9f), Math.min(lightness, 0.6f));
    }

    /**
     * 计算最佳字体大小
     */
    private static int calculateOptimalFontSize(int imageSize) {
        // 根据图像大小动态计算字体大小
        if (imageSize <= 32) return 12;
        if (imageSize <= 48) return 16;
        if (imageSize <= 64) return 20;
        return 24;
    }
}