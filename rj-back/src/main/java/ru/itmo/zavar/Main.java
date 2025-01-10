package ru.itmo.zavar;

import java.util.logging.Logger;

import reactor.core.scheduler.Schedulers;
import ru.itmo.zavar.websocket.WebSocketServer;

public class Main {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    public static void main(String[] args) throws InterruptedException {
        World world = new World();

        WebSocketServer server = new WebSocketServer(world, 8088);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("received SIGINT");
            server.stop();
            world.stop();
            Schedulers.shutdownNow();
        }));

        server.start();
    }
}
