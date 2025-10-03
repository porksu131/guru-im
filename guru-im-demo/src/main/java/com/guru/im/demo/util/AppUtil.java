package com.guru.im.demo.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppUtil {
    public static final String APP_NAME = "GuruIM";

    public static String getAppDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        Path path;

        if (os.contains("win")) {
            // Windows: AppData/Roaming
            path = Paths.get(System.getenv("APPDATA"));
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support
            path = Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            // Linux/Unix: ~/.config
            path = Paths.get(System.getProperty("user.home"), ".config");
        }

        // 创建应用专属目录
        Path appDir = path.resolve(APP_NAME);
        try {
            Files.createDirectories(appDir);
        } catch (IOException e) {
            System.err.println("创建应用目录时出错: " + e.getMessage());
        }
        return appDir.toString() + File.separator;
    }
}
