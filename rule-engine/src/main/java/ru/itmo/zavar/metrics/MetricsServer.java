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
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
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
