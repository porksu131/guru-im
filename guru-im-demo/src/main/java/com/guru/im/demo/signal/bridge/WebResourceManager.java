package com.guru.im.demo.signal.bridge;

import com.guru.im.demo.signal.windows.AbstractMediaWindow;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WebResourceManager {
    protected static final Logger logger = LoggerFactory.getLogger(WebResourceManager.class);

    // 静态资源目录
    private static File mediaDir;
    private static boolean isDevMode = false;

    static {
        try {
            URL codeSource = AbstractMediaWindow.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSource != null && "file".equals(codeSource.getProtocol())) {
                File jarFile = new File(codeSource.toURI());
                if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                    // 生产模式：JAR 运行
                    File jarDir = jarFile.getParentFile();
                    mediaDir = new File(jarDir, "media-page");
                    ensureMediaResources(mediaDir, jarFile.lastModified());
                } else {
                    // 开发模式：可能运行在 target/classes 或 IDEA 的 out 目录
                    isDevMode = true;
                    // 直接使用 classpath 资源路径（不做解压）
                }
            } else {
                // 其他情况（如 jar 协议）也当作开发模式处理
                isDevMode = true;
            }
        } catch (Exception e) {
            logger.error("初始化媒体目录失败", e);
            // 降级：尝试使用当前工作目录
            mediaDir = new File(System.getProperty("user.dir"), "media-page");
        }
    }

    /**
     * 确保媒体资源已解压到目标目录
     * @param targetDir 目标目录
     * @param jarModified JAR文件最后修改时间
     */
    private static void ensureMediaResources(File targetDir, long jarModified) {
        File versionFile = new File(targetDir.getParentFile(), ".version");
        boolean needExtract = true;

        // 检查版本是否匹配
        if (targetDir.exists() && targetDir.isDirectory() && versionFile.exists()) {
            try {
                String savedVer = new String(Files.readAllBytes(versionFile.toPath())).trim();
                if (savedVer.equals(String.valueOf(jarModified))) {
                    needExtract = false; // 版本一致，无需解压
                }
            } catch (IOException ignored) {}
        }

        if (needExtract) {
            try {
                // 删除旧目录（如果有）
                if (targetDir.exists()) {
                    FileUtils.deleteDirectory(targetDir); // 需要 Apache Commons IO 或手写递归删除
                }
                targetDir.mkdirs();
                extractResourcesFromJar("media-page", targetDir);
                // 写入版本文件
                Files.write(versionFile.toPath(), String.valueOf(jarModified).getBytes());
                logger.info("媒体资源解压到 {} 完成", targetDir.getAbsolutePath());
            } catch (IOException e) {
                logger.error("解压媒体资源失败", e);
            }
        } else {
            logger.info("媒体资源已存在，版本匹配，跳过解压");
        }
    }

    /**
     * 从 JAR 复制整个目录到目标文件夹
     */
    private static void extractResourcesFromJar(String resourceFolder, File destDir) throws IOException {
        URL jarUrl = AbstractMediaWindow.class.getProtectionDomain().getCodeSource().getLocation();
        if (!"file".equals(jarUrl.getProtocol())) return;
        try (JarFile jar = new JarFile(new File(jarUrl.toURI()))) {
            Enumeration<JarEntry> entries = jar.entries();
            String prefix = resourceFolder + "/";
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(prefix) && !entry.isDirectory()) {
                    String relativePath = name.substring(prefix.length());
                    File destFile = new File(destDir, relativePath);
                    destFile.getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public static String getHtmlPageUrlFromWebServer(String htmlFile) throws IOException {
        // 启动http服务器
        EmbeddedHttpServer.start();
        // 使用 HTTP 服务器地址
        String baseUrl = "http://localhost:" + EmbeddedHttpServer.getPort() + "/media-page/" + htmlFile;
        // 参数直接拼接（也可以在 JS 中从 window.location 获取）
        return baseUrl;
    }


    public static String getHtmlPageUrl(String htmlFile, String resourceFolder) throws IOException {
        try {
            if (isDevMode) {
                // 开发模式：从 classpath 直接获取文件路径
                URL resource = WebResourceManager.class.getClassLoader().getResource(resourceFolder + "/" + htmlFile);
                if (resource != null && "file".equals(resource.getProtocol())) {
                    String path = URLDecoder.decode(resource.getPath(), "UTF-8");
                    return "file:///" + path.replace("\\", "/");
                }
            }

            // 生产模式：使用解压后的固定目录
            if (mediaDir != null) {
                File targetFile = new File(mediaDir, htmlFile);
                if (targetFile.exists()) {
                    return "file:///" + targetFile.getAbsolutePath().replace("\\", "/");
                } else {
                    ensureMediaResources(mediaDir, getJarModifiedTime());
                    if (targetFile.exists()) {
                        return "file:///" + targetFile.getAbsolutePath().replace("\\", "/");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取URL失败", e);
        }

        return getHtmlPageUrlFromWebServer(htmlFile);
    }

    private static long getJarModifiedTime() {
        try {
            URL jarUrl = AbstractMediaWindow.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl != null && "file".equals(jarUrl.getProtocol())) {
                return new File(jarUrl.toURI()).lastModified();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
