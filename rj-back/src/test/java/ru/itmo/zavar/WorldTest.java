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

        Postoffice postOffice1 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 10));

        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");
        Mail mail1 = new Mail(0, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail2 = new Mail(1, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail3 = new Mail(2, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail4 = new Mail(3, "subject1", actor1, actor1, new Mail.Data("text"), 10);

        world.postoffices.add(postOffice1);
        world.actors.addActor(actor1);

        world.actors.sendMail(actor1.getId(), postOffice1.getId(), mail1);
        world.actors.sendMail(actor1.getId(), postOffice1.getId(), mail2);
        world.actors.sendMail(actor1.getId(), postOffice1.getId(), mail3);
        world.actors.sendMail(actor1.getId(), postOffice1.getId(), mail4);
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMail(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()));
        world.actors.receiveMail(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.sendMail(actor1.getId(), postOffice1.getId(), mail1);
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMail(actor1.getId(), postOffice1.getId(), m -> logger.fine(m.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.stop();
        Schedulers.shutdownNow();
    }

    @Test
    public void sendReceiveFlux() throws InterruptedException {
        World world = new World();

        Postoffice postOffice1 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 10));
        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");

        world.postoffices.add(postOffice1);
        world.actors.addActor(actor1);

        Actor.Schedule.Task task1 = world.actors.sendMailFlux(actor1.getId(), postOffice1.getId(),
                Flux.generate(AtomicLong::new, (state, sink) -> {
                    long i = state.getAndIncrement();
                    Mail mail = new Mail(i, "subject1", actor1, actor1, new Mail.Data("text"), 10);
                    sink.next(mail);
                    return state;
                }).delayElements(Duration.ofMillis(500)).cast(Mail.class));

        world.actors.receiveMailMany(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMailMany(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(5L));

        // stopping task
        world.actors.deleteActorTask(actor1.getId(), task1.getId());
        Thread.sleep(Duration.ofSeconds(1L));

        // waiting a little to read latest messages
        world.actors.receiveMailMany(actor1.getId(), postOffice1.getId(), m -> logger.info(m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(2L));

        world.stop();
        Schedulers.shutdownNow();
    }

    @Test
    public void transportRoute() throws InterruptedException {
        World world = new World();

        Postoffice postOffice1 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"),
                new Coordinates(0, 5));
        Postoffice postOffice2 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188034"), "S1", "12"),
                new Coordinates(5, 0));
        Postoffice postOffice3 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "188034"), "S1", "12"),
                new Coordinates(0, 10));

        Route<Postoffice> route1 = new Route<Postoffice>(List.of(postOffice1, postOffice2), true);
        Route<Postoffice> route2 = new Route<Postoffice>(List.of(postOffice1, postOffice3), true);

        Transport transport1 = new Transport(10, 5D, 10, "T1");

        world.postoffices.add(postOffice1);
        world.postoffices.add(postOffice2);

        world.transports.add(transport1);
        world.transports.setRoute(transport1.getId(), route1);
        world.transports.start(transport1.getId());
        Thread.sleep(Duration.ofSeconds(5L));

        world.transports.stop(transport1.getId());
        world.transports.start(transport1.getId(), 0);
        Thread.sleep(Duration.ofSeconds(4L));

        world.transports.stop(transport1.getId());
        world.transports.setRoute(transport1.getId(), route2);
        world.transports.start(transport1.getId());
        Thread.sleep(Duration.ofSeconds(4L));

        world.transports.stop(transport1.getId());
        Thread.sleep(Duration.ofSeconds(1L));

        world.stop();
        Schedulers.shutdownNow();
    }

    @Test
    public void postOfficeTransfer() throws InterruptedException {
        World world = new World();

        Postoffice postOffice1 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "1"), "S1", "12"),
                new Coordinates(0, 5));
        Postoffice postOffice2 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "2"), "S1", "12"),
                new Coordinates(5, 0));
        Postoffice postOffice3 = new Postoffice(new Address(new PostalAddress("Russia", "SPb", "3"), "S1", "12"),
                new Coordinates(5, 0));

        Route<Postoffice> route1 = new Route<Postoffice>(List.of(postOffice1, postOffice2, postOffice3), true);

        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "1"), "S3", "14"),
                "A1", "B1", "C1");
        Actor actor2 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "2"), "S3", "14"),
                "A1", "B1", "C1");

        Transport transport1 = new Transport(10, 5D, 10, "T1");

        world.postoffices.add(postOffice1);
        world.postoffices.add(postOffice2);
        world.postoffices.add(postOffice3);
        world.transports.add(transport1);
        world.transports.setRoute(transport1.getId(), route1);
        world.transports.start(transport1.getId());
        world.actors.addActor(actor1);
        world.actors.addActor(actor2);

        Actor.Schedule.Task task1 = world.actors.sendMailFlux(actor1.getId(), postOffice1.getId(),
                Flux.generate(AtomicLong::new, (state, sink) -> {
                    long i = state.getAndIncrement();
                    Mail mail = new Mail(i, "from actor1 to actor2 (need to transfer)", actor1, actor2,
                            new Mail.Data(""), 1);
                    sink.next(mail);
                    return state;
                }).delayElements(Duration.ofMillis(500)).cast(Mail.class));

        Actor.Schedule.Task task2 = world.actors.sendMailFlux(actor2.getId(), postOffice1.getId(),
                Flux.generate(AtomicLong::new, (state, sink) -> {
                    long i = state.getAndIncrement();
                    Mail mail = new Mail(i, "from actor2 to actor1 (no need to transfer)", actor2, actor1,
                            new Mail.Data(""), 2);
                    sink.next(mail);
                    return state;
                }).delayElements(Duration.ofMillis(500)).cast(Mail.class));

        Thread.sleep(Duration.ofSeconds(5L));

        world.actors.deleteActorTask(actor1.getId(), task1.getId());
        world.actors.deleteActorTask(actor2.getId(), task2.getId());
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMailMany(actor2.getId(), postOffice2.getId(),
                m -> logger.info("received actor2 " + m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        // should throw warnings
        world.actors.receiveMailMany(actor1.getId(), postOffice2.getId(),
                m -> logger.info("received actor1 " + m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.actors.receiveMailMany(actor1.getId(), postOffice1.getId(),
                m -> logger.info("received actor1 " + m.toString()),
                e -> logger.warning(e.toString()));
        Thread.sleep(Duration.ofSeconds(1L));

        world.stop();
        Schedulers.shutdownNow();
    }
}
