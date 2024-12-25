package ru.itmo.zavar;

import java.time.Duration;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import reactor.core.scheduler.Schedulers;

public class WorldTest {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    @Test
    public void test1() throws InterruptedException {
        World world = new World();

        PostOffice postOffice1 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"));

        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");
        Mail mail1 = new Mail(0, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail2 = new Mail(1, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail3 = new Mail(2, "subject1", actor1, actor1, new Mail.Data("text"), 10);
        Mail mail4 = new Mail(3, "subject1", actor1, actor1, new Mail.Data("text"), 10);

        world.offices.add(postOffice1);
        world.actors.add(actor1);

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
    public void test2() throws InterruptedException {
        World world = new World();

        world.stop();
        Schedulers.shutdownNow();
    }
}
