package ru.itmo.zavar;

import java.util.logging.Logger;
import ru.itmo.zavar.websocket.WebSocketServer;

public class Main {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    public static void main(String[] args) throws InterruptedException {
        WebSocketServer server = new WebSocketServer(8088);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("received SIGINT");
            server.stop();
        }));

        server.start();
    }
}
