package com.guru.im.demo.util;

import org.nutz.img.Images;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * 高质量头像生成工具类
 */
public class AvatarGenerator {

    private AvatarGenerator() {
        // 工具类，禁止实例化
    }

    /**
     * 生成圆形头像
     */
    public static ImageIcon createCircleAvatar(String userName, String avatarUrl, int size) {
        try {
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                BufferedImage bufferedImage = ImageIO.read(new URL(avatarUrl));
                return createPerfectRoundedAvatar(bufferedImage);
            } else {
                // 生成基于名称的默认头像
                return createPerfectNameAvatar(userName, size);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 圆形头像生成
     */
    private static ImageIcon createPerfectRoundedAvatar(BufferedImage sourceImage) {
        BufferedImage circleBufferImage = createCircleWithAlpha(sourceImage);
        return new ImageIcon(circleBufferImage);
    }

    /**
     * 生成基于名称的头像
     */
    private static ImageIcon createPerfectNameAvatar(String userName, int size) {
        if (userName == null || userName.trim().isEmpty()) {
            userName = "User";
        }

        try {
            // 直接生成目标尺寸的头像，避免缩放

            BufferedImage avatarImage = Images.createAvatar(
                    getDisplayText(userName),   // 只显示前两个字符
                    size,                   // 直接生成目标尺寸
                    "rgba(255,255,255,1)",  // 白色文字
                    generatePerfectBgColor(userName), // 高质量背景色
                    "Microsoft YaHei",      // 字体
                    calculateOptimalFontSize(size), // 优化字体大小
                    Font.BOLD
            );

            // 转换为圆形
            return createPerfectRoundedAvatar(avatarImage);
        } catch (Exception e) {
            return createHighQualityFallbackAvatar(userName, size);
        }
    }

    /**
     * 高质量备用头像
     */
    private static ImageIcon createHighQualityFallbackAvatar(String name, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        ImageUtil.setHighQualityRenderingHints(g2);

        // 绘制圆形背景
        Color bgColor = generateColorFromName(name);
        g2.setColor(bgColor);
        g2.fillOval(0, 0, size, size);

        // 绘制文字
        String displayText = getDisplayText(name);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Microsoft YaHei", Font.BOLD, calculateOptimalFontSize(size)));

        FontMetrics fm = g2.getFontMetrics();
        int textX = (size - fm.stringWidth(displayText)) / 2;
        int textY = (size - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(displayText, textX, textY);

        g2.dispose();
        return new ImageIcon(image);
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
     * 生成高质量背景色
     */
    private static String generatePerfectBgColor(String name) {
        Color color = generateColorFromName(name);
        return String.format("rgba(%d,%d,%d,0.95)", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 基于名称生成颜色
     */
    private static Color generateColorFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return new Color(79, 129, 189); // 好看的默认蓝色
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

    public static BufferedImage createCircleWithAlpha(BufferedImage image) {
        int size = Math.min(image.getWidth(), image.getHeight());
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // 高质量设置
        ImageUtil.setHighQualityRenderingHints(g2);

        // 创建圆形蒙版
        BufferedImage mask = createCircularMask(size);

        // 使用AlphaComposite进行混合
        g2.drawImage(image, 0, 0, size, size, null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
        g2.drawImage(mask, 0, 0, null);

        g2.dispose();
        return output;
    }

    private static BufferedImage createCircularMask(int size) {
        BufferedImage mask = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mask.createGraphics();

        ImageUtil.setHighQualityRenderingHints(g2);

        // 绘制白色圆形（完全透明）
        g2.setColor(new Color(255, 255, 255, 255));
        g2.fillOval(0, 0, size, size);

        g2.dispose();
        return mask;
    }
}