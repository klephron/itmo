package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import java.time.Duration;
import java.util.logging.Level;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        World world = new World();

        PostOffice postOffice1 = new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12"));
        Actor actor1 = new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1");
        Mail mail = new Mail(0, "subject1", actor1, actor1, new Mail.Data("text"), 10);

        world.offices.add(postOffice1);
        world.actors.add(actor1);

        logger.log(Level.INFO, "size: " + postOffice1.getMailsToSend().size());

        world.actors.sendMail(actor1, postOffice1, mail);
        world.actors.sendMail(actor1, postOffice1, mail);
        Thread.sleep(Duration.ofSeconds(1L));

        logger.log(Level.INFO, "size: " + postOffice1.getMailsToSend().size());

        world.actors.sendMail(actor1, postOffice1, mail);
        Thread.sleep(Duration.ofSeconds(1L));

        logger.log(Level.INFO, "size: " + postOffice1.getMailsToSend().size());
        world.actors.sendMail(actor1, postOffice1, mail);
        Thread.sleep(Duration.ofSeconds(1L));

        logger.log(Level.INFO, "size: " + postOffice1.getMailsToSend().size());
    }
}
