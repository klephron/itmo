package ru.itmo.zavar;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class WorldTest {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    @Test
    public void sendReceiveOne() throws InterruptedException {
        World world = new World();

        PostOffice postOffice1 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 10));

        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");
        Mail mail1 = new Mail(0, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail2 = new Mail(1, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail3 = new Mail(2, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail4 = new Mail(3, "subject1", actor1, actor1, new Mail.Data("text"), 10);

        world.offices.add(postOffice1);
        world.actors.addActor(actor1);

        world.actors.sendMail(actor1, postOffice1, mail1);
        world.actors.sendMail(actor1, postOffice1, mail2);
        world.actors.sendMail(actor1, postOffice1, mail3);
        world.actors.sendMail(actor1, postOffice1, mail4);
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMail(actor1, postOffice1, m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1, postOffice1, m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1, postOffice1, m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1, postOffice1, m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1, postOffice1, m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.sendMail(actor1, postOffice1, mail1);
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMail(actor1, postOffice1, m -> logger.fine(m.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.stop();
        Schedulers.shutdownNow();
    }

    @Test
    public void sendReceiveFlux() throws InterruptedException {
        World world = new World();

        PostOffice postOffice1 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 10));
        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");

        world.offices.add(postOffice1);
        world.actors.addActor(actor1);

        Actor.Schedule.Task task1 = world.actors.sendMailFlux(actor1, postOffice1,
                Flux.generate(AtomicLong::new, (state, sink) -> {
                    long i = state.getAndIncrement();
                    Mail mail = new Mail(i, "subject1", actor1, actor1, new Mail.Data("text"), 10);
                    sink.next(mail);
                    return state;
                }).delayElements(Duration.ofMillis(500)).cast(Mail.class));

        world.actors.receiveMailMany(actor1, postOffice1, m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMailMany(actor1, postOffice1, m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(5L));

        // stopping task
        world.actors.deleteActorTask(actor1, task1);
        Thread.sleep(Duration.ofSeconds(1L));

        // waiting a little to read latest messages
        world.actors.receiveMailMany(actor1, postOffice1, m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(2L));

        world.stop();
        Schedulers.shutdownNow();
    }

    @Test
    public void transportRoute() throws InterruptedException {
        World world = new World();

        PostOffice postOffice1 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 5));
        PostOffice postOffice2 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188034"), "S1", "12"),
                new Coordinates(5, 0));
        PostOffice postOffice3 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188034"), "S1", "12"),
                new Coordinates(0, 10));

        Route<PostOffice> route1 = new Route<PostOffice>(List.of(postOffice1, postOffice2), true);
        Route<PostOffice> route2 = new Route<PostOffice>(List.of(postOffice1, postOffice3), true);

        Transport transport1 = new Transport(10, 5D);

        world.offices.add(postOffice1);
        world.offices.add(postOffice2);

        world.transports.add(transport1);
        world.transports.setRoute(transport1, route1);
        world.transports.start(transport1);
        Thread.sleep(Duration.ofSeconds(5L));

        world.transports.stop(transport1);
        world.transports.start(transport1, 0);
        Thread.sleep(Duration.ofSeconds(4L));

        world.transports.stop(transport1);
        world.transports.setRoute(transport1, route2);
        world.transports.start(transport1);
        Thread.sleep(Duration.ofSeconds(4L));

        world.transports.stop(transport1);
        Thread.sleep(Duration.ofSeconds(1L));

        world.stop();
        Schedulers.shutdownNow();
    }

    @Test
    public void postOfficeTransfer() throws InterruptedException {
        World world = new World();

        PostOffice postOffice1 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 5));
        PostOffice postOffice2 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188034"), "S1", "12"),
                new Coordinates(5, 0));

        Route<PostOffice> route1 = new Route<PostOffice>(List.of(postOffice1, postOffice2), true);

        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");

        Transport transport1 = new Transport(10, 5D);

        world.offices.add(postOffice1);
        world.offices.add(postOffice2);
        world.transports.add(transport1);
        world.transports.setRoute(transport1, route1);
        world.transports.start(transport1);

        world.actors.addActor(actor1);
        Actor.Schedule.Task task1 = world.actors.sendMailFlux(actor1, postOffice1,
                Flux.generate(AtomicLong::new, (state, sink) -> {
                    long i = state.getAndIncrement();
                    Mail mail = new Mail(i, "subject1", actor1, actor1, new Mail.Data("text"), 10);
                    sink.next(mail);
                    return state;
                }).delayElements(Duration.ofMillis(500)).cast(Mail.class));

        Thread.sleep(Duration.ofSeconds(5L));

        world.actors.deleteActorTask(actor1, task1);
        Thread.sleep(Duration.ofSeconds(1L));

        world.stop();
        Schedulers.shutdownNow();
    }
}
