package dev.javaplatform.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Minimal service used to exercise the platform end to end.
 *
 * <p>Platform contract: listen on {@code server.port} (the systemd unit passes
 * {@code -Dserver.port=$SERVER_PORT}) and answer {@code GET /health} with 200.
 */
public final class App {

    private final HttpServer server;

    App(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> respond(exchange, 200, "ok"));
        server.createContext("/", exchange -> respond(exchange, 200,
                "java-app " + version() + " (" + env("APP_ENVIRONMENT", "local") + ")"));
    }

    void start() {
        server.start();
    }

    void stop() {
        server.stop(0);
    }

    int port() {
        return server.getAddress().getPort();
    }

    static String version() {
        String v = App.class.getPackage().getImplementationVersion();
        return v != null ? v : "dev";
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        App app = new App(Integer.getInteger("server.port", 8080));
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        app.start();
        System.out.println("java-app " + version() + " listening on " + app.port());
    }
}
