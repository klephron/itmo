package ru.itmo.zavar;

import java.time.Duration;
import java.util.logging.Logger;

import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServer;

public class Main {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    public static void main(String[] args) throws InterruptedException {
        HttpServer.create()
                .port(12021)
                .route(routes -> routes.get("/stream",
                        (request, response) -> {
                            logger.info("sending data chunks...");
                            return response.sendString(Flux.interval(Duration.ofSeconds(1))
                                    .map(i -> "Data chunk " + i + "\n"));
                        }))
                .bindNow()
                .onDispose()
                .block();
    }
}
