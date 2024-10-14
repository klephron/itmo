package ru.itmo.zavar.benchmark;

import org.openjdk.jmh.annotations.*;
import ru.itmo.zavar.actor.Address;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailGenerator;
import ru.itmo.zavar.stats.MailStatistics;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MailBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private final MailGenerator generator = new MailGenerator();
        private final MailStatistics stats = new MailStatistics();
        private final IndividualActor actorToCheck = new IndividualActor("Alexander", "Stepanov", "Pavlovich",
            new Address("Russia", "Moscow", "Moscow Oblast", "412 Walnut St", "19984"));
        @Param({"5000", "15000", "25000", "50000", "100000", "250000"})
        private int count;
        private final int delay = 500;
        private Set<Mail> mailSet;

        @Setup(Level.Invocation)
        public void setup() {
            mailSet = generator.generateMails(count);
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    public void statsBenchmarkWithDelayAndWithParallelAndWithSpliterator(BenchmarkState state) {
        try {
            state.stats.reportByReceiverActor(state.actorToCheck, state.mailSet, state.delay, true, true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    public void statsBenchmarkWithDelayAndWithParallel(BenchmarkState state) {
        try {
            state.stats.reportByReceiverActor(state.actorToCheck, state.mailSet, state.delay, true, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    public void statsBenchmarkWithDelay(BenchmarkState state) {
        try {
            state.stats.reportByReceiverActor(state.actorToCheck, state.mailSet, state.delay, false, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    public void statsBenchmarkWithoutDelayAndWithParallel(BenchmarkState state) {
        try {
            state.stats.reportByReceiverActor(state.actorToCheck, state.mailSet, 0, true, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 2)
    @Measurement(iterations = 3)
    public void statsBenchmarkWithoutDelay(BenchmarkState state) {
        try {
            state.stats.reportByReceiverActor(state.actorToCheck, state.mailSet, 0, false, false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
