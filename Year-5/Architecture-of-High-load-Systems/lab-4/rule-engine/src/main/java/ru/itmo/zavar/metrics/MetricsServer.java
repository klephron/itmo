package ru.itmo.zavar.metrics;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public final class MetricsServer {
    private final HttpServer server;
    private final int port;

    public MetricsServer(final int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
        this.server.createContext("/metrics", exchange -> {
            String response = MetricsConfig.getRegistry().scrape();
            byte[] responseBytes = response.getBytes();

            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");

            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });
    }

    public void start() {
        this.server.start();
        LoggerWrapper.log(Level.INFO, "MetricsServer started on port {}", this.port);
    }

    public void stop() {
        this.server.stop(0);
    }
}
