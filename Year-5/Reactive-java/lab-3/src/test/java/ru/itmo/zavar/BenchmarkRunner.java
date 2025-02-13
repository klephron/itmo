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
    }
}
