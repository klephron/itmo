package ru.itmo.zavar;

import ru.itmo.zavar.actor.Address;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailGenerator;
import ru.itmo.zavar.stats.MailStatisticsReactive;

import java.util.Set;

public class FlowableTest {
    public static void main(String[] args) {
        MailGenerator mailGenerator = new MailGenerator();
        Set<Mail> mailSet = mailGenerator.generateMails(200000);
        MailStatisticsReactive statsReactive = new MailStatisticsReactive();
        IndividualActor actorToCheck = new IndividualActor("Alexander", "Stepanov", "Pavlovich",
            new Address("Russia", "Moscow", "Moscow Oblast", "412 Walnut St", "19984"));
        statsReactive.countByReceiverWithBackpressure(actorToCheck, mailSet).blockingSubscribe(new MailStatisticsReactive.CustomMailSubscriber());
    }
}
