package com.guru.im.demo.gui.component;// VideoPlayer.java

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoPlayer extends JPanel {
    // 视频组件
    private VideoPanel videoPanel;
    private JLabel statusLabel;
    private JLabel timeLabel;

    // 控制按钮
    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton openFileButton;
    private JButton openURLButton;

    // 控制条
    private JSlider progressSlider;
    private JSlider volumeSlider;
    private AtomicBoolean isSeeking = new AtomicBoolean(false);
    private AtomicBoolean userSeeking = new AtomicBoolean(false);

    // 视频播放相关
    private FFmpegFrameGrabber grabber;
    private VideoPlaybackThread playbackThread;
    private AudioPlaybackThread audioThread;
    private AtomicBoolean isPlaying;
    private AtomicBoolean isPaused;

    // 音频相关
    private SourceDataLine audioLine;
    private AudioFormat audioFormat;
    private LinkedBlockingQueue<AudioFrame> audioQueue;
    private float volume = 0.1f; // 默认音量10%

    // 视频信息
    private long videoDuration = 0; // 微秒
    private double frameRate;
    private int videoWidth;
    private int videoHeight;
    private boolean hasAudio;
    private int audioChannels;
    private int sampleRate;

    // 同步控制
    private volatile long currentVideoTime = 0;
    private volatile long audioStartTime = 0;
    private volatile long videoStartTime = 0;
    private volatile long lastUpdateTime = 0;

    // 音频帧数据结构
    private static class AudioFrame {
        byte[] data;
        long timestamp;

        AudioFrame(byte[] data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    public VideoPlayer() {
        initializeComponents();
        setupEventHandlers();
        initializeVideoSystem();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());

        // 视频显示面板
        videoPanel = new VideoPanel();
        add(videoPanel, BorderLayout.CENTER);

        // 控制面板
        add(createControlPanel(), BorderLayout.SOUTH);

        // 工具栏
        //add(createToolBar(), BorderLayout.NORTH);

        // 初始化状态
        updateStatus("就绪 - 请打开视频文件或URL");
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEtchedBorder());

        JPanel progressPanel = new JPanel(new BorderLayout(0, 0));
        progressPanel.setOpaque(false);
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        timeLabel = new JLabel("00:00:00 / 00:00:00");
        timeLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        timePanel.add(timeLabel);
        progressPanel.add(timePanel, BorderLayout.NORTH);

        // 进度条
        progressSlider = new JSlider(0, 1000, 0);
        progressSlider.setPreferredSize(new Dimension(600, 20));
        progressSlider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        progressSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                userSeeking.set(true);
                // 立即更新进度条位置到点击位置
                JSlider source = (JSlider) e.getSource();
                Point p = e.getPoint();
                double percent = p.x / (double) source.getWidth();
                int newValue = (int) (percent * (source.getMaximum() - source.getMinimum())) + source.getMinimum();
                source.setValue(newValue);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (grabber != null && !isSeeking.get()) {
                    double position = progressSlider.getValue() / 1000.0;
                    seekToPosition(position);
                }
                userSeeking.set(false);
            }
        });

        // 添加变化监听器
        progressSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!progressSlider.getValueIsAdjusting() && userSeeking.get()) {
                    double position = progressSlider.getValue() / 1000.0;
                    seekToPosition(position);
                    userSeeking.set(false);
                }
            }
        });
        progressPanel.add(progressSlider, BorderLayout.CENTER);

        controlPanel.add(progressPanel, BorderLayout.NORTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setOpaque(false);

        playButton = createIconButton("▶", "播放", new Color(34, 139, 34));
        buttonPanel.add(playButton);

        pauseButton = createIconButton("⏸", "暂停", new Color(218, 165, 32));
        buttonPanel.add(pauseButton);

        stopButton = createIconButton("⏹", "停止", new Color(178, 34, 34));
        buttonPanel.add(stopButton);

        JLabel volumeIcon = new JLabel("🔊");
        buttonPanel.add(volumeIcon);

        volumeSlider = new JSlider(0, 100, 10);
        volumeSlider.setPreferredSize(new Dimension(80, 20));
        volumeSlider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        volumeSlider.addChangeListener(e -> {
            volume = volumeSlider.getValue() / 100.0f;
            updateVolume();
        });
        buttonPanel.add(volumeSlider);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        controlPanel.add(statusLabel, BorderLayout.SOUTH);

        return controlPanel;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        openFileButton = new JButton("打开文件");
        openFileButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        openFileButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toolBar.add(openFileButton);

        openURLButton = new JButton("打开URL");
        openURLButton.setIcon(UIManager.getIcon("FileView.computerIcon"));
        openURLButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toolBar.add(openURLButton);

        return toolBar;
    }

    private JButton createIconButton(String icon, String tooltip, Color color) {
        JButton button = new JButton(icon);
        button.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        button.setToolTipText(tooltip);
        //button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(50, 30));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder());
        return button;
    }

    private void setupEventHandlers() {
        playButton.addActionListener(e -> play());
        pauseButton.addActionListener(e -> pause());
        stopButton.addActionListener(e -> stop());
        //openFileButton.addActionListener(e -> openFile());
        //openURLButton.addActionListener(e -> openURL());
    }

    private void initializeVideoSystem() {
        isPlaying = new AtomicBoolean(false);
        isPaused = new AtomicBoolean(false);
        audioQueue = new LinkedBlockingQueue<>(1000);

        avutil.av_log_set_level(avutil.AV_LOG_ERROR);

        updateControls();
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "视频文件 (*.mp4, *.avi, *.mkv, *.mov, *.wmv, *.flv)",
                "mp4", "avi", "mkv", "mov", "wmv", "flv");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadVideo(selectedFile.getAbsolutePath());
        }
    }

    private void openURL() {
        String url = JOptionPane.showInputDialog(this,
                "请输入视频URL:", "打开网络视频", JOptionPane.QUESTION_MESSAGE);
        if (url != null && !url.trim().isEmpty()) {
            loadVideo(url.trim());
        }
    }

    public void loadVideo(String source) {
        if (isPlaying.get()) {
            // 防止重复点击
            return;
        }
        stop();

        try {
            updateStatus("正在加载视频...");
            videoPanel.setLoading(true);

            grabber = new FFmpegFrameGrabber(source);
            grabber.setOption("stimeout", "5000000");
            grabber.setPixelFormat(avutil.AV_PIX_FMT_BGR24);

            grabber.start();

            // 获取视频信息
            videoDuration = grabber.getLengthInTime();
            frameRate = grabber.getFrameRate();
            videoWidth = grabber.getImageWidth();
            videoHeight = grabber.getImageHeight();
            audioChannels = grabber.getAudioChannels();
            sampleRate = grabber.getSampleRate();
            hasAudio = audioChannels > 0 && sampleRate > 0;

            // 设置默认值
            if (frameRate <= 0) frameRate = 25;
            if (sampleRate <= 0) sampleRate = 44100;
            if (audioChannels <= 0) audioChannels = 2;
            if (videoDuration <= 0) {
                videoDuration = Long.MAX_VALUE / 2;
            }

            // 抓取并显示首帧
            Frame firstFrame = grabber.grabImage();
            if (firstFrame != null && firstFrame.image != null) {
                final BufferedImage thumbnail = Java2DFrameUtils.toBufferedImage(firstFrame);
                SwingUtilities.invokeLater(() -> videoPanel.setImage(thumbnail));
                // 重置grabber到开始位置
                grabber.setTimestamp(0);
            }

            updateStatus(String.format("视频加载成功: %s [%dx%d] %.1fFPS %s",
                    grabber.getFormat(), videoWidth, videoHeight, frameRate,
                    hasAudio ? "有音频" : "无音频"));

            if (hasAudio) {
                initializeAudio();
            }

            progressSlider.setValue(0);
            currentVideoTime = 0;
            updateTimeDisplay(0, videoDuration);
            updateControls();

        } catch (Exception e) {
            videoPanel.setLoading(false);
            updateStatus("加载视频失败: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "无法加载视频: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            videoPanel.setLoading(false);
        }
    }

    private void initializeAudio() throws LineUnavailableException {
        audioFormat = new AudioFormat(sampleRate, 16, audioChannels, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        if (!AudioSystem.isLineSupported(info)) {
            audioFormat = new AudioFormat(44100, 16, 2, true, false);
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
        }

        audioLine = (SourceDataLine) AudioSystem.getLine(info);
        audioLine.open(audioFormat, 44100 * 2 * 2);
        updateVolume();

        audioQueue.clear();
        audioStartTime = 0;
    }

    private void play() {
        if (grabber == null) {
            updateStatus("错误: 没有加载视频");
            return;
        }

        if (isPlaying.get() && isPaused.get()) {
            // 继续播放
            isPaused.set(false);
            if (hasAudio && audioLine != null) {
                audioLine.start();
            }
            // 重新校准时间基准
            long currentTime = System.nanoTime();
            videoStartTime = currentTime - (currentVideoTime * 1000);
            audioStartTime = currentTime - (currentVideoTime * 1000);
            updateStatus("继续播放");
        } else if (!isPlaying.get()) {
            // 开始新播放
            isPlaying.set(true);
            isPaused.set(false);

            long currentTime = System.nanoTime();
            videoStartTime = currentTime - (currentVideoTime * 1000);
            audioStartTime = currentTime - (currentVideoTime * 1000);

            playbackThread = new VideoPlaybackThread();
            playbackThread.start();

            if (hasAudio) {
                audioThread = new AudioPlaybackThread();
                audioThread.start();
                if (audioLine != null) {
                    audioLine.start();
                }
            }

            updateStatus("正在播放");
        }

        updateControls();
    }

    private void pause() {
        if (isPlaying.get() && !isPaused.get()) {
            isPaused.set(true);
            if (hasAudio && audioLine != null) {
                audioLine.stop();
            }
            updateStatus("已暂停");
            updateControls();
        }
    }

    void stop() {
        isPlaying.set(false);
        isPaused.set(false);
        isSeeking.set(false);
        userSeeking.set(false);

        // 停止音频线
        if (audioLine != null) {
            audioLine.stop();
            audioLine.flush();
        }

        // 停止线程
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
        }

        if (audioThread != null) {
            audioThread.interrupt();
            try {
                audioThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            audioThread = null;
        }

        // 清理资源
        if (audioLine != null) {
            audioLine.close();
            audioLine = null;
        }

        if (audioQueue != null) {
            audioQueue.clear();
        }

        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            grabber = null;
        }

        videoPanel.clear();
        progressSlider.setValue(0);
        currentVideoTime = 0;
        updateTimeDisplay(0, videoDuration);
        updateStatus("已停止");
        updateControls();
    }

    private void seekToPosition(double position) {
        if (grabber == null || isSeeking.get()) return;

        isSeeking.set(true);
        videoPanel.setLoading(true);

        try {
            long targetTime = (long) (videoDuration * position);
            updateStatus("跳转到: " + formatTime(targetTime));

            boolean wasPaused = isPaused.get();
            isPaused.set(true);

            if (hasAudio && audioLine != null) {
                audioLine.stop();
                audioLine.flush();
            }

            // 清空音频队列
            audioQueue.clear();

            // 设置新的时间位置
            grabber.setTimestamp(targetTime);
            currentVideoTime = targetTime;

            // 更新显示
            updateTimeDisplay(targetTime, videoDuration);

            // 重置时间基准
            long currentTime = System.nanoTime();
            videoStartTime = currentTime - (currentVideoTime * 1000);
            audioStartTime = currentTime - (currentVideoTime * 1000);

            // 恢复播放状态
            isPaused.set(wasPaused);
            if (!wasPaused && hasAudio && audioLine != null) {
                audioLine.start();
            }

        } catch (Exception e) {
            updateStatus("跳转失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isSeeking.set(false);
            videoPanel.setLoading(false);
        }
    }

    private void updateVolume() {
        if (audioLine != null && audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl gainControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                float dB = (float) (Math.log(volume == 0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
                dB = Math.max(min, Math.min(max, dB));
                gainControl.setValue(dB);
            } catch (Exception e) {
                System.err.println("音量控制失败: " + e.getMessage());
            }
        }
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private void updateTimeDisplay(long currentTime, long totalTime) {
        SwingUtilities.invokeLater(() -> {
            String current = formatTime(currentTime);
            String total = formatTime(totalTime);
            timeLabel.setText(current + " / " + total);

            if (!userSeeking.get()) {
                int progress = totalTime > 0 ? (int) ((currentTime * 1000) / totalTime) : 0;
                progressSlider.setValue(progress);
            }
        });
    }

    private String formatTime(long microseconds) {
        if (microseconds <= 0) return "00:00:00";
        if (microseconds >= Long.MAX_VALUE / 2) return "直播流";

        long seconds = microseconds / 1000000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private void updateControls() {
        boolean hasVideo = (grabber != null);
        boolean playing = isPlaying.get();
        boolean paused = isPaused.get();

        playButton.setEnabled(hasVideo && (!playing || paused));
        pauseButton.setEnabled(hasVideo && playing && !paused);
        stopButton.setEnabled(hasVideo && playing);
        //openFileButton.setEnabled(!playing);
        //openURLButton.setEnabled(!playing);
        progressSlider.setEnabled(hasVideo);
    }

    // 视频播放线程 - 保持音频原始性
    private class VideoPlaybackThread extends Thread {
        private static final long TARGET_FRAME_TIME_NS = 1000000000L / 60;

        @Override
        public void run() {
            try {
                while (isPlaying.get() && !Thread.interrupted()) {
                    if (isPaused.get()) {
                        Thread.sleep(50);
                        continue;
                    }

                    if (isSeeking.get()) {
                        Thread.sleep(10);
                        continue;
                    }

                    long frameStartTime = System.nanoTime();

                    Frame frame = null;
                    try {
                        frame = grabber.grab();
                    } catch (Exception grabException) {
                        System.err.println("抓取帧失败: " + grabException.getMessage());
                        if (grabber.getTimestamp() >= videoDuration - 1000000) {
                            break;
                        } else {
                            Thread.sleep(50);
                            continue;
                        }
                    }

                    if (frame == null) {
                        if (grabber.getTimestamp() >= videoDuration - 1000000) {
                            break;
                        } else {
                            Thread.sleep(10);
                            continue;
                        }
                    }

                    // 基于系统时间的简单同步
                    long elapsedNano = System.nanoTime() - videoStartTime;
                    currentVideoTime = elapsedNano / 1000;

                    if (frame.image != null) {
                        try {
                            final BufferedImage image = Java2DFrameUtils.toBufferedImage(frame);
                            SwingUtilities.invokeLater(() -> videoPanel.setImage(image));

                            long now = System.currentTimeMillis();
                            if (now - lastUpdateTime > 100) {
                                updateTimeDisplay(currentVideoTime, videoDuration);
                                lastUpdateTime = now;
                            }

                            // 简单的帧率控制
                            long frameTime = System.nanoTime() - frameStartTime;
                            long sleepTime = TARGET_FRAME_TIME_NS - frameTime;
                            if (sleepTime > 1000000) {
                                Thread.sleep(sleepTime / 1000000, (int) (sleepTime % 1000000));
                            }

                        } catch (Exception imageException) {
                            System.err.println("处理视频帧失败: " + imageException.getMessage());
                            continue;
                        }
                    } else if (frame.samples != null && hasAudio && !isSeeking.get()) {
                        // 直接处理音频，不做复杂的时间戳调整
                        processAudioFrame(frame);
                    }
                }

                // 播放结束处理
                if (isPlaying.get()) {
                    SwingUtilities.invokeLater(() -> {
                        if (currentVideoTime >= videoDuration - 2000000) {
                            stop();
                            updateStatus("播放完成");
                        } else {
                            updateStatus("播放异常结束");
                            stop();
                        }
                    });
                }

            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    SwingUtilities.invokeLater(() -> {
                        updateStatus("播放错误: " + e.getMessage());
                        stop();
                    });
                    e.printStackTrace();
                }
            }
        }

        private void processAudioFrame(Frame frame) {
            try {
                Buffer buffer = (Buffer) frame.samples[0];
                byte[] audioData = convertAudioBufferToBytes(buffer);

                if (audioData != null && audioData.length > 0) {
                    // 使用简单的当前时间戳，不做复杂调整
                    AudioFrame audioFrame = new AudioFrame(audioData, currentVideoTime);

                    if (!audioQueue.offer(audioFrame, 50, TimeUnit.MILLISECONDS)) {
                        // 队列满时简单处理
                        while (audioQueue.size() > 500) {
                            audioQueue.poll();
                        }
                        audioQueue.offer(audioFrame);
                    }
                }
            } catch (Exception e) {
                System.err.println("处理音频帧失败: " + e.getMessage());
            }
        }

        private byte[] convertAudioBufferToBytes(Buffer buffer) {
            try {
                if (buffer instanceof ByteBuffer) {
                    ByteBuffer byteBuffer = (ByteBuffer) buffer;
                    byte[] data = new byte[byteBuffer.remaining()];
                    byteBuffer.get(data);
                    return data;
                } else if (buffer instanceof ShortBuffer) {
                    ShortBuffer shortBuffer = (ShortBuffer) buffer;
                    int samples = shortBuffer.remaining();
                    byte[] data = new byte[samples * 2];
                    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                    byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

                    while (shortBuffer.hasRemaining()) {
                        short sample = shortBuffer.get();
                        byteBuffer.putShort(sample);
                    }
                    return data;
                }
            } catch (Exception e) {
                System.err.println("音频数据转换失败: " + e.getMessage());
            }
            return new byte[0];
        }
    }

    // 音频播放线程 - 保持简单
    private class AudioPlaybackThread extends Thread {
        @Override
        public void run() {
            try {
                while (isPlaying.get() && !Thread.interrupted()) {
                    if (isPaused.get() || isSeeking.get()) {
                        Thread.sleep(50);
                        continue;
                    }

                    AudioFrame audioFrame = audioQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (audioFrame != null && audioLine != null && audioLine.isOpen()) {
                        // 直接播放，不做复杂处理
                        audioLine.write(audioFrame.data, 0, audioFrame.data.length);
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    System.err.println("音频播放错误: " + e.getMessage());
                }
            }
        }
    }

    // 视频显示面板
    private class VideoPanel extends JPanel {
        private BufferedImage currentImage;
        private boolean isLoading = false;

        public VideoPanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(800, 600));
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        public void setImage(BufferedImage image) {
            this.currentImage = image;
            repaint();
        }

        public void clear() {
            this.currentImage = null;
            this.isLoading = false;
            repaint();
        }

        public void setLoading(boolean loading) {
            this.isLoading = loading;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (currentImage != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imgWidth = currentImage.getWidth();
                int imgHeight = currentImage.getHeight();

                double scale = Math.min((double) panelWidth / imgWidth, (double) panelHeight / imgHeight);
                int scaledWidth = (int) (imgWidth * scale);
                int scaledHeight = (int) (imgHeight * scale);
                int x = (panelWidth - scaledWidth) / 2;
                int y = (panelHeight - scaledHeight) / 2;

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(currentImage, x, y, scaledWidth, scaledHeight, null);

                if (isLoading) {
                    g2d.setColor(new Color(0, 0, 0, 128));
                    g2d.fillRect(0, 0, panelWidth, panelHeight);

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("微软雅黑", Font.BOLD, 16));
                    String message = "正在加载...";
                    FontMetrics fm = g2d.getFontMetrics();
                    int textX = (panelWidth - fm.stringWidth(message)) / 2;
                    int textY = panelHeight / 2;
                    g2d.drawString(message, textX, textY);
                }
            } else {
                g.setColor(Color.GRAY);
                g.setFont(new Font("微软雅黑", Font.PLAIN, 18));
                String message = "请打开视频";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(message)) / 2;
                int y = getHeight() / 2;
                g.drawString(message, x, y);
            }
        }
    }
}