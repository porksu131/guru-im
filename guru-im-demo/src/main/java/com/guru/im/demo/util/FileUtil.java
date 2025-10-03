package com.guru.im.demo.util;

import com.guru.im.protocol.model.ImageContent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public class FileUtil {

    public static final int FILE_TYPE_IMAGE = 1;
    public static final int FILE_TYPE_AUDIO = 2;
    public static final int FILE_TYPE_VIDEO = 3;
    public static final int FILE_TYPE_OTHER = 4;

    // 支持的图片格式
    static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
    private static final String IMAGE_DESCRIPTION = "图片文件 (*.jpg, *.jpeg, *.png, *.gif, *.bmp, *.webp)";

    // 支持的视频格式
    static final String[] VIDEO_EXTENSIONS = {"mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"};
    private static final String VIDEO_DESCRIPTION = "视频文件 (*.mp4, *.avi, *.mov, *.wmv, *.flv, *.mkv, *.webm)";

    // 支持的音频格式
    static final String[] AUDIO_EXTENSIONS = {"mp3", "wav", "ogg", "m4a", "aac", "flac", "wma"};
    private static final String AUDIO_DESCRIPTION = "音频文件 (*.mp3, *.wav, *.ogg, *.m4a, *.aac, *.flac, *.wma)";

    // 使用Preferences保存配置（无需额外依赖）
    private static final Preferences USER_PREFERENCES = Preferences.userNodeForPackage(FileUtil.class);
    static final String LAST_IMAGE_DIRECTORY_KEY = "LAST_IMAGE_DIRECTORY";
    static final String LAST_AUDIO_DIRECTORY_KEY = "LAST_AUDIO_DIRECTORY";
    static final String LAST_VIDEO_DIRECTORY_KEY = "LAST_VIDEO_DIRECTORY";
    static final String LAST_OTHER_DIRECTORY_KEY = "LAST_FILE_DIRECTORY";

    public interface FileSelectionCallback {
        void onFilesSelected(File[] selectedFiles);
    }

    /**
     * 打开文件选择器并过滤指定类型的文件，记住上次选择的目录
     */
    public static File[] selectFiles(Component parent, int fileType) {
        JFileChooser fileChooser = createAndConfigureFileChooser(fileType);
        // 打开文件对话框
        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();

            // 保存最后选择的目录
            updateLastOpenDirectory(selectedFiles, fileType);

            return selectedFiles;
        }

        return new File[0];
    }

    /**
     * 创建非模态文件选择对话框
     */
    public static void selectFilesNonModal(Component parent, int fileType, FileUtil.FileSelectionCallback callback) {
        Frame parentFrame = getParentFrame(parent);
        JDialog dialog = new JDialog(parentFrame, "选择文件", false); // false表示非模态
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JFileChooser fileChooser = createAndConfigureFileChooser(fileType);
        fileChooser.addActionListener(e -> {
            if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                // 快速获取文件（在事件线程中）
                File[] selectedFiles = fileChooser.getSelectedFiles();

                // 保存最后选择的目录
                updateLastOpenDirectory(selectedFiles, fileType);

                // 关闭对话框
                dialog.dispose();

                // 回调
                SwingUtilities.invokeLater(() -> callback.onFilesSelected(selectedFiles));

            } else if (e.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                dialog.dispose();
            }
        });

        dialog.setLayout(new BorderLayout());
        dialog.add(fileChooser, BorderLayout.CENTER);
        dialog.setVisible(true); // 非阻塞调用，立即返回
    }

    private static JFileChooser createAndConfigureFileChooser(int fileType) {
        JFileChooser chooser = new JFileChooser();

        // 基本配置
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("选择文件");

        // 设置初始目录
        setInitialDirectory(chooser, fileType);

        // 设置文件过滤器
        setupFileFilter(chooser, fileType);

        return chooser;
    }

    /**
     * 设置初始目录
     */
    private static void setInitialDirectory(JFileChooser chooser, int fileType) {
        String lastDir = getLastDirectoryFromPreferences(fileType);
        if (lastDir != null && !lastDir.trim().isEmpty()) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setCurrentDirectory(dir);
                return;
            }
        }
        // 默认使用用户主目录
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
    }

    /**
     * 设置文件过滤器
     */
    private static void setupFileFilter(JFileChooser chooser, int fileType) {
        switch (fileType) {
            case FILE_TYPE_IMAGE:
                FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(IMAGE_DESCRIPTION, IMAGE_EXTENSIONS);
                chooser.setFileFilter(imageFilter);
                chooser.setAcceptAllFileFilterUsed(false);
                break;

            case FILE_TYPE_AUDIO:
                FileNameExtensionFilter audioFilter = new FileNameExtensionFilter(AUDIO_DESCRIPTION, AUDIO_EXTENSIONS);
                chooser.setFileFilter(audioFilter);
                chooser.setAcceptAllFileFilterUsed(false);
                break;

            case FILE_TYPE_VIDEO:
                FileNameExtensionFilter videoFilter = new FileNameExtensionFilter(VIDEO_DESCRIPTION, VIDEO_EXTENSIONS);
                chooser.setFileFilter(videoFilter);
                chooser.setAcceptAllFileFilterUsed(false);
                break;

            default:
                chooser.setAcceptAllFileFilterUsed(true);
                break;
        }
    }


    private static String getLastDirectoryFromPreferences(int fileType) {
        switch (fileType) {
            case FILE_TYPE_IMAGE:
                return USER_PREFERENCES.get(LAST_IMAGE_DIRECTORY_KEY, null);
            case FILE_TYPE_AUDIO:
                return USER_PREFERENCES.get(LAST_AUDIO_DIRECTORY_KEY, null);
            case FILE_TYPE_VIDEO:
                return USER_PREFERENCES.get(LAST_VIDEO_DIRECTORY_KEY, null);
            default:
                return USER_PREFERENCES.get(LAST_OTHER_DIRECTORY_KEY, null);
        }
    }

    /**
     * 获取父窗口Frame
     */
    private static Frame getParentFrame(Component parent) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        return (window instanceof Frame) ? (Frame) window : null;
    }

    /**
     * 保存目录到偏好设置
     */
    private static void updateLastOpenDirectory(File[] selectedFiles, int fileType) {
        // 保存最后选择的目录
        if (selectedFiles.length > 0) {
            File firstFile = selectedFiles[0];
            File parentDir = firstFile.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                if (fileType == FILE_TYPE_IMAGE) {
                    USER_PREFERENCES.put(LAST_IMAGE_DIRECTORY_KEY, parentDir.getAbsolutePath());
                } else if (fileType == FILE_TYPE_AUDIO) {
                    USER_PREFERENCES.put(LAST_AUDIO_DIRECTORY_KEY, parentDir.getAbsolutePath());
                }else if (fileType == FILE_TYPE_VIDEO) {
                    USER_PREFERENCES.put(LAST_VIDEO_DIRECTORY_KEY, parentDir.getAbsolutePath());
                } else {
                    USER_PREFERENCES.put(LAST_OTHER_DIRECTORY_KEY, parentDir.getAbsolutePath());
                }

            }
        }
    }

    /**
     * 获取文件类型
     */
    public static int getFileType(File file) {
        String fileName = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                return FILE_TYPE_IMAGE;
            }
        }
        for (String ext : VIDEO_EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                return FILE_TYPE_VIDEO;
            }
        }
        return FILE_TYPE_OTHER;
    }

    public static ImageContent.ImageFormat getImageFormat(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ImageContent.ImageFormat.UNKNOWN_IMAGE;
        }

        // 获取文件扩展名（不区分大小写）
        String extension = getFileExtension(filename).toLowerCase();

        switch (extension) {
            case "jpg":
            case "jpeg":
                return ImageContent.ImageFormat.JPEG;
            case "png":
                return ImageContent.ImageFormat.PNG;
            case "gif":
                return ImageContent.ImageFormat.GIF;
            case "webp":
                return ImageContent.ImageFormat.WEBP;
            default:
                return ImageContent.ImageFormat.UNKNOWN_IMAGE;
        }
    }

    // 辅助方法：获取文件扩展名
    public static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1);
    }

    public static String getFileMainName(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return filename.substring(0, lastDotIndex);
    }

    public static void openFolder(String filePath) {
        try {
            File file = new File(filePath);
            File folder = file.getParentFile();

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(folder);
            } else {
                System.out.println("当前系统不支持桌面操作");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openFolderAndSelectFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows
                new ProcessBuilder("explorer", "/select,", file.getAbsolutePath()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String truncateFileName(String fileName, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(fileName) <= maxWidth) {
            return fileName;
        }

        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);

        // 如果连省略号都显示不下，直接返回省略号
        if (ellipsisWidth > maxWidth) {
            return ellipsis;
        }

        // 分离文件名和扩展名
        String nameWithoutExt;
        String extension;
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            // 有有效的扩展名
            nameWithoutExt = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex); // 包含点号，如 ".txt"
        } else {
            // 没有扩展名或无效的扩展名
            nameWithoutExt = fileName;
            extension = "";
        }

        // 计算扩展名宽度
        int extWidth = fm.stringWidth(extension);
        int availableWidth = maxWidth - ellipsisWidth - extWidth;

        // 如果连扩展名都显示不下，只返回扩展名（如果可能）
        if (availableWidth < 0) {
            if (extWidth <= maxWidth) {
                return extension;
            } else {
                return ellipsis;
            }
        }

        // 如果文件名部分太短，不需要中间省略
        if (fm.stringWidth(nameWithoutExt) <= availableWidth) {
            return nameWithoutExt + extension;
        }

        // 文件名部分采用中间省略：保留开头和结尾
        for (int totalChars = nameWithoutExt.length() - 1; totalChars >= 2; totalChars--) {
            for (int prefixLength = 1; prefixLength < totalChars; prefixLength++) {
                int suffixLength = totalChars - prefixLength;

                // 确保不越界
                if (suffixLength <= 0 || prefixLength + suffixLength > nameWithoutExt.length()) {
                    continue;
                }

                String prefix = nameWithoutExt.substring(0, prefixLength);
                String suffix = nameWithoutExt.substring(nameWithoutExt.length() - suffixLength);
                String candidate = prefix + ellipsis + suffix + extension;

                if (fm.stringWidth(candidate) <= maxWidth) {
                    return candidate;
                }
            }
        }

        // 如果连一个字符都显示不下，只显示扩展名（如果可能）
        if (extWidth <= maxWidth) {
            return ellipsis + extension;
        } else {
            return ellipsis;
        }
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}