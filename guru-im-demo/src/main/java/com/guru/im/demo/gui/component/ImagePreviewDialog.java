package com.guru.im.demo.gui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class ImagePreviewDialog extends JFrame {
    private JLabel imageLabel;
    private JScrollPane imageScrollPane;
    private double scale = 1.0;
    private double rotation = 0.0;
    private BufferedImage originalImage;
    private String imageName;
    private JToolBar toolBar;
    
    public ImagePreviewDialog(JFrame parent) {
        super("图片预览");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        
        setSize(800, 600);
        setLocationRelativeTo(parent);
        
        initComponents();
        setupLayout();
        setupListeners();
    }
    
    private void initComponents() {
        imageLabel = new JLabel("", JLabel.CENTER);
        imageLabel.setBackground(new Color(45, 45, 45));
        imageLabel.setOpaque(true);
        
        imageScrollPane = new JScrollPane(imageLabel);
        imageScrollPane.getHorizontalScrollBar().setUI(new CustomScrollBar());
        imageScrollPane.getVerticalScrollBar().setUI(new CustomScrollBar());
        imageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        imageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        imageScrollPane.setBorder(BorderFactory.createEmptyBorder());
        imageScrollPane.getViewport().setBackground(new Color(45, 45, 45));
    }

    private void setupLayout() {
        // 创建工具栏
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(new Color(60, 60, 60));
        toolBar.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));

        JButton zoomInButton = createToolBarButton("放大", "➕");
        JButton zoomOutButton = createToolBarButton("缩小", "➖");
        JButton originalSizeButton = createToolBarButton("原始尺寸", "⭕");
        JButton fitToScreenButton = createToolBarButton("适应窗口", "📐");
        JButton rotateButton = createToolBarButton("旋转", "🔄");

        // 在工具栏左侧添加弹性空间
        toolBar.add(Box.createHorizontalGlue());

        // 添加按钮
        toolBar.add(zoomInButton);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0))); // 按钮间距
        toolBar.add(zoomOutButton);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));
        toolBar.add(originalSizeButton);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));
        toolBar.add(fitToScreenButton);
        toolBar.add(Box.createRigidArea(new Dimension(10, 0)));
        toolBar.add(rotateButton);

        // 在工具栏右侧添加弹性空间
        toolBar.add(Box.createHorizontalGlue());

        // 设置布局
        setLayout(new BorderLayout());
        add(imageScrollPane, BorderLayout.CENTER);  // 图片在中间
        add(toolBar, BorderLayout.SOUTH);           // 工具栏在底部（现在会居中）

        // 添加按钮事件
        zoomInButton.addActionListener(e -> zoomIn());
        zoomOutButton.addActionListener(e -> zoomOut());
        originalSizeButton.addActionListener(e -> setOriginalSize());
        fitToScreenButton.addActionListener(e -> fitToScreen());
        rotateButton.addActionListener(e -> rotateImage());
    }

    private JButton createToolBarButton(String tooltip, String text) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setBackground(new Color(80, 80, 80));
        button.setForeground(Color.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(100, 100, 100));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(80, 80, 80));
            }
        });

        return button;
    }
    
    private void setupListeners() {
        // 添加鼠标滚轮缩放
        imageLabel.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }
        });
        
        // 添加组件监听器，在窗口大小变化时自动适应图片
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (originalImage != null) {
                    // 如果当前不是原始尺寸，在窗口大小变化时重新适应
                    SwingUtilities.invokeLater(() -> {
                        fitToScreen();
                    });
                }
            }
        });
        
        // 添加键盘快捷键
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKey, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        // F11 全屏切换
        KeyStroke f11Key = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f11Key, "F11");
        getRootPane().getActionMap().put("F11", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFullScreen();
            }
        });
    }
    
    private void toggleFullScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        
        if (gd.getFullScreenWindow() == null) {
            // 进入全屏
            dispose();
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            // 退出全屏
            gd.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setVisible(true);
        }
    }
    
    public void showImage(String imageName, BufferedImage bufferedImage) {
        this.imageName = imageName;
        this.originalImage = bufferedImage;
        this.scale = 1.0;
        this.rotation = 0.0;
        
        try {
            if (originalImage != null) {
                setTitle("图片预览 - " + imageName);
                // 首次打开时自动适应窗口大小
                SwingUtilities.invokeLater(() -> {
                    fitToScreen();
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "无法加载图片", "错误", JOptionPane.ERROR_MESSAGE);
        }

        setVisible(true);
    }
    
    private void updateImage() {
        if (originalImage == null) return;
        
        try {
            // 应用旋转
            BufferedImage rotatedImage = rotateImage(originalImage, rotation);
            
            // 应用缩放
            int newWidth = (int) (rotatedImage.getWidth() * scale);
            int newHeight = (int) (rotatedImage.getHeight() * scale);
            
            Image scaledImage = rotatedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaledImage);
            imageLabel.setIcon(icon);
            
            // 更新窗口标题显示缩放比例
            setTitle("图片预览 - " + imageName +
                    String.format(" (%.0f%%)", scale * 100));
            
            // 重新验证和重绘
            imageLabel.revalidate();
            imageLabel.repaint();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private BufferedImage rotateImage(BufferedImage image, double angle) {
        if (angle == 0.0) return image;
        
        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        
        int newWidth = (int) Math.floor(image.getWidth() * cos + image.getHeight() * sin);
        int newHeight = (int) Math.floor(image.getHeight() * cos + image.getWidth() * sin);
        
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = rotated.createGraphics();
        
        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 旋转图像
        AffineTransform transform = new AffineTransform();
        transform.translate(newWidth / 2, newHeight / 2);
        transform.rotate(radians);
        transform.translate(-image.getWidth() / 2, -image.getHeight() / 2);
        
        g2d.setTransform(transform);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        
        return rotated;
    }
    
    private void zoomIn() {
        if (scale < 5.0) {
            scale *= 1.2;
            updateImage();
        }
    }
    
    private void zoomOut() {
        if (scale > 0.1) {
            scale /= 1.2;
            updateImage();
        }
    }
    
    private void setOriginalSize() {
        scale = 1.0;
        updateImage();
    }
    
    private void fitToScreen() {
        if (originalImage == null) return;
        
        try {
            // 获取当前可用的显示区域（考虑工具栏和边框）
            Dimension viewportSize = imageScrollPane.getViewport().getExtentSize();
            int availableWidth = viewportSize.width - 40; // 留一些边距
            int availableHeight = viewportSize.height - 40;
            
            // 计算适合窗口的缩放比例
            double widthScale = (double) availableWidth / originalImage.getWidth();
            double heightScale = (double) availableHeight / originalImage.getHeight();
            
            scale = Math.min(widthScale, heightScale);
            
            // 如果图片比窗口小，保持原始大小
            if (scale > 1.0) {
                scale = 1.0;
            }
            
            updateImage();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void rotateImage() {
        rotation += 90.0;
        if (rotation >= 360.0) {
            rotation = 0.0;
        }
        updateImage();
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // 显示窗口时居中
            setLocationRelativeTo(getOwner());
            // 确保窗口有合适的初始大小
            if (getWidth() < 400 || getHeight() < 300) {
                setSize(800, 600);
            }
        }
        super.setVisible(visible);
    }
}