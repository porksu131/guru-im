package com.guru.im.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    protected static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Properties props = new Properties();
    private static final String CONFIG_FILE_NAME = "app.properties";  // 配置文件名

    // 静态代码块，类加载时自动执行
    static {
        loadConfig();
    }

    // 获取 EXE 或 JAR 所在的真实目录
    public static String getAppPath() {
        try {
            String path = ConfigManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            // 如果是 JAR 包，返回的是 jar 的完整路径；如果是 exe，返回的是 exe 的路径
            return new File(path).getParent();//（即 exe 的上一级目录）
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }

    private static void loadConfig() {
        // 1. 首先尝试从 JAR 同级目录加载外部配置文件
        String baseDir = getAppPath();
        File externalFile = new File(baseDir, CONFIG_FILE_NAME);
        if (externalFile.exists() && externalFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                props.load(fis);
                logger.info("加载外部配置文件成功：{}", externalFile.getAbsolutePath());
                return;
            } catch (IOException e) {
                logger.error("加载外部配置文件失败，将使用内部默认配置：{}", e.getMessage());
            }
        }

        // 2. 外部文件不存在或加载失败，则从 JAR 包内的资源加载（即 classpath）
        try (InputStream is = ConfigManager.class.getResourceAsStream("/" + CONFIG_FILE_NAME)) {
            if (is != null) {
                props.load(is);
            } else {
                logger.error("找不到默认配置文件，请检查资源路径");
            }
        } catch (IOException e) {
            logger.error("加载内部配置文件失败：{}", e.getMessage());
        }
    }

    /**
     * 获取字符串配置值，若不存在则返回默认值
     */
    public static String getString(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * 获取字符串配置值，若不存在则返回 null
     */
    public static String getString(String key) {
        return props.getProperty(key);
    }

    /**
     * 重新加载配置文件（用于外部文件被修改后动态刷新）
     */
    public static void reload() {
        props.clear();
        loadConfig();
    }
}
