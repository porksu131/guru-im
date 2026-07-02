package com.guru.im.demo.signal.bridge;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class EmbeddedHttpServer {
    private static HttpServer server;
    private static int port = 8765; // 可配置

    public static void start() throws IOException {
        if(server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // 将 /media-page 映射到 classpath 下的 media-page
        server.createContext("/media-page", new ClasspathResourceHandler("/media-page"));
        server.setExecutor(null);
        server.start();
        System.out.println("HTTP Server started on port " + port);
    }

    // 自定义 Handler 读取 classpath 资源
    public static class ClasspathResourceHandler implements HttpHandler {
        private final String basePath;
        public ClasspathResourceHandler(String basePath) { this.basePath = basePath; }
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath(); // /media-page/audio-call.html
            String resourcePath = basePath + path.substring(basePath.length());
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.sendResponseHeaders(200, in.available());
                try (OutputStream os = exchange.getResponseBody()) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    public static int getPort() {
        return port;
    }
}