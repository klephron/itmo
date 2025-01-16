package ru.itmo.zavar;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import ru.itmo.zavar.model.*;
import ru.itmo.zavar.model.actor.Actor;
import ru.itmo.zavar.model.actor.IndividualActor;
import ru.itmo.zavar.model.actor.OrganizationActor;
import ru.itmo.zavar.model.address.Address;
import ru.itmo.zavar.model.address.PostalAddress;
import ru.itmo.zavar.websocket.WebSocketServer;

import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    public static void main(String[] args) throws InterruptedException {
        World world = new World();
        genericWorld(world);

        WebSocketServer server = new WebSocketServer(world, 8088);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("received SIGINT");
            server.stop();
            world.stop();
            Schedulers.shutdownNow();
        }));

        server.start();
    }

    public static void genericWorld(World world) {
        Postoffice postoffice1 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 10));
        Postoffice postoffice2 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188034"), "S1", "12"),
                new Coordinates(10, 0));

        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                10, "A1", "B1", "C1");
        Actor actor2 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188034"), "S3", "14"),
                10, "A2", "B2", "C2");
        Actor actor3 = new OrganizationActor(new Address(new PostalAddress("Russia", "SPb", "188034"), "S3", "14"),
                10, "O3");

        Transport transport1 = new Transport(10, 5D, 10, "T1");
        Transport transport2 = new Transport(20, 10D, 20, "T2");
        Transport transport3 = new Transport(30, 5D, 2, "T3");

        world.postoffices.add(postoffice1);
        world.postoffices.add(postoffice2);
        world.actors.addActor(actor1);
        world.actors.addActor(actor2);
        world.actors.addActor(actor3);
        world.transports.add(transport1);
        world.transports.add(transport2);
        world.transports.add(transport3);
    }
}
