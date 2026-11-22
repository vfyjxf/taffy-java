package dev.vfyjxf.taffy.gentest;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Minimal static HTTP server used to load WPT pages (which reference resources
 * through absolute URLs such as {@code /resources/testharness.js}).
 */
final class LocalHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final Path root;
    private final Path cssRoot;
    private final int port;

    private LocalHttpServer(HttpServer server, Path root, Path cssRoot, int port) {
        this.server = server;
        this.root = root;
        this.cssRoot = cssRoot;
        this.port = port;
    }

    static LocalHttpServer start(Path root, Path cssRoot) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        LocalHttpServer wrapper = new LocalHttpServer(
                server,
                root.toAbsolutePath().normalize(),
                cssRoot != null ? cssRoot.toAbsolutePath().normalize() : null,
                server.getAddress().getPort());
        server.createContext("/", wrapper::handle);
        server.start();
        return wrapper;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + port + "/";
    }

    private void handle(HttpExchange exchange) throws IOException {
        String rawPath = Objects.toString(exchange.getRequestURI().getRawPath(), "/");
        String path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.isBlank()) {
            sendText(exchange, 404, "Not Found");
            return;
        }
        Path file = resolveRequestPath(path);
        if (file == null || !Files.exists(file) || Files.isDirectory(file)) {
            sendText(exchange, 404, "Not Found");
            return;
        }
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentTypeFor(file));
        headers.set("Cache-Control", "no-store");
        byte[] bytes = Files.readAllBytes(file);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private Path resolveRequestPath(String path) {
        String p = path.replace('\\', '/');
        if (cssRoot != null && p.startsWith("css/")) {
            Path fromCssRoot = safeResolve(cssRoot, p);
            if (fromCssRoot != null && Files.exists(fromCssRoot)) {
                return fromCssRoot;
            }
        }
        return safeResolve(root, p);
    }

    private static Path safeResolve(Path base, String path) {
        Path file = base.resolve(path).normalize();
        if (!file.startsWith(base)) {
            return null;
        }
        return file;
    }

    private static void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String contentTypeFor(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xhtml") || name.endsWith(".xht")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (name.endsWith(".ttf")) {
            return "font/ttf";
        }
        if (name.endsWith(".woff")) {
            return "font/woff";
        }
        if (name.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
