package ru.itmo.zavar;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.itmo.zavar.actor.Address;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.benchmark.MailBenchmark;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailGenerator;
import ru.itmo.zavar.stats.MailStatisticsReactive;

import java.util.Set;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(BenchmarkRunner.class.getSimpleName())
            .include(MailBenchmark.class.getSimpleName())
            .resultFormat(ResultFormatType.JSON)
            .result("result.json")
            .build();

        new Runner(opt).run();
//        MailGenerator mailGenerator = new MailGenerator();
//        Set<Mail> mailSet = mailGenerator.generateMails(200000);
//        MailStatisticsReactive statsReactive = new MailStatisticsReactive();
//        IndividualActor actorToCheck = new IndividualActor("Alexander", "Stepanov", "Pavlovich",
//            new Address("Russia", "Moscow", "Moscow Oblast", "412 Walnut St", "19984"));
//        statsReactive.countByReceiverWithBackpressure(actorToCheck, mailSet).blockingSubscribe(new MailStatisticsReactive.CustomMailSubscriber());
    }
}
