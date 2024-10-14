package ru.itmo.zavar.stats;

import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailStatus;
import ru.itmo.zavar.mail.mark.Mark;
import ru.itmo.zavar.stats.collector.actor.ReceiverCollector;
import ru.itmo.zavar.stats.collector.actor.SenderCollector;
import ru.itmo.zavar.stats.collector.mark.MarkCollector;
import ru.itmo.zavar.stats.collector.status.MailStatusCollector;
import ru.itmo.zavar.stats.data.ReportContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class MailStatistics {

    public ReportContainer<Long> reportAvailableByStatus(MailStatus status, Set<Mail> mailSet) {
        long iterationValue = 0;
        Instant iterationStart = Instant.now();
        for (Mail mail : mailSet) {
            if (status == mail.getStatus()) {
                iterationValue++;
            }
        }
        Instant iterationEnd = Instant.now();
        long iterationElapsedTime = Duration.between(iterationStart, iterationEnd).toNanos();

        long streamApiValue;
        Instant streamApiStart = Instant.now();
        streamApiValue = mailSet.stream().filter(mail -> status.equals(mail.getStatus())).count();
        Instant streamApiEnd = Instant.now();
        long streamApiElapsedTime = Duration.between(streamApiStart, streamApiEnd).toNanos();

        long customCollectorValue;
        Instant customCollectorApiStart = Instant.now();
        customCollectorValue = mailSet.stream().collect(new MailStatusCollector()).get(status);
        Instant customCollectorApiEnd = Instant.now();
        long customCollectorApiTime = Duration.between(customCollectorApiStart, customCollectorApiEnd).toNanos();

        return new ReportContainer<>(iterationValue, streamApiValue, customCollectorValue,
                iterationElapsedTime, streamApiElapsedTime, customCollectorApiTime);
    }

    public ReportContainer<Long> reportAvailableByMark(Mark mark, Set<Mail> mailSet) {
        long iterationValue = 0;
        Instant iterationStart = Instant.now();
        for (Mail mail : mailSet) {
            if (mail.getMarks().contains(mark)) {
                iterationValue++;
            }
        }
        Instant iterationEnd = Instant.now();
        long iterationElapsedTime = Duration.between(iterationStart, iterationEnd).toNanos();

        long streamApiValue;
        Instant streamApiStart = Instant.now();
        streamApiValue = mailSet.stream().filter(mail -> mail.getMarks().contains(mark)).count();
        Instant streamApiEnd = Instant.now();
        long streamApiElapsedTime = Duration.between(streamApiStart, streamApiEnd).toNanos();

        long customCollectorValue;
        Instant customCollectorApiStart = Instant.now();
        customCollectorValue = mailSet.stream().collect(new MarkCollector(mark)).mailsWithMark();
        Instant customCollectorApiEnd = Instant.now();
        long customCollectorApiTime = Duration.between(customCollectorApiStart, customCollectorApiEnd).toNanos();

        return new ReportContainer<>(iterationValue, streamApiValue, customCollectorValue,
                iterationElapsedTime, streamApiElapsedTime, customCollectorApiTime);
    }

    public ReportContainer<Long> reportBySenderActor(IndividualActor actor, Set<Mail> mailSet) {
        long iterationValue = 0;
        Instant iterationStart = Instant.now();
        for (Mail mail : mailSet) {
            if (mail.getSender().equals(actor)) {
                iterationValue++;
            }
        }
        Instant iterationEnd = Instant.now();
        long iterationElapsedTime = Duration.between(iterationStart, iterationEnd).toNanos();

        long streamApiValue;
        Instant streamApiStart = Instant.now();
        streamApiValue = mailSet.stream().filter(mail -> mail.getSender().equals(actor)).count();
        Instant streamApiEnd = Instant.now();
        long streamApiElapsedTime = Duration.between(streamApiStart, streamApiEnd).toNanos();

        long customCollectorValue;
        Instant customCollectorApiStart = Instant.now();
        customCollectorValue = mailSet.stream().collect(new SenderCollector(actor));
        Instant customCollectorApiEnd = Instant.now();
        long customCollectorApiTime = Duration.between(customCollectorApiStart, customCollectorApiEnd).toNanos();

        return new ReportContainer<>(iterationValue, streamApiValue, customCollectorValue,
                iterationElapsedTime, streamApiElapsedTime, customCollectorApiTime);
    }

    public ReportContainer<Long> reportByReceiverActor(IndividualActor actor, Set<Mail> mailSet) {
        long iterationValue = 0;
        Instant iterationStart = Instant.now();
        for (Mail mail : mailSet) {
            if (mail.getReceiver().equals(actor)) {
                iterationValue++;
            }
        }
        Instant iterationEnd = Instant.now();
        long iterationElapsedTime = Duration.between(iterationStart, iterationEnd).toNanos();

        long streamApiValue;
        Instant streamApiStart = Instant.now();
        streamApiValue = mailSet.stream().filter(mail -> mail.getReceiver().equals(actor)).count();
        Instant streamApiEnd = Instant.now();
        long streamApiElapsedTime = Duration.between(streamApiStart, streamApiEnd).toNanos();

        long customCollectorValue;
        Instant customCollectorApiStart = Instant.now();
        customCollectorValue = mailSet.stream().collect(new ReceiverCollector(actor));
        Instant customCollectorApiEnd = Instant.now();
        long customCollectorApiTime = Duration.between(customCollectorApiStart, customCollectorApiEnd).toNanos();

        return new ReportContainer<>(iterationValue, streamApiValue, customCollectorValue,
                iterationElapsedTime, streamApiElapsedTime, customCollectorApiTime);
    }

    public ReportContainer<Long> reportByReceiverActor(IndividualActor actor, Set<Mail> mailSet, long delay, boolean parallel, boolean customSpliterator) throws InterruptedException {
        long iterationValue = 0;
        Instant iterationStart = Instant.now();
        for (Mail mail : mailSet) {
            if (mail.getReceiver().equals(actor)) {
                iterationValue++;
            }
        }
        Instant iterationEnd = Instant.now();
        long iterationElapsedTime = Duration.between(iterationStart, iterationEnd).toNanos();

        long streamApiValue;
        Instant streamApiStart = Instant.now();
        if (!parallel) {
            streamApiValue = mailSet.stream().filter(mail -> mail.getReceiver().equals(actor)).count();
        } else {
            if (!customSpliterator) {
                streamApiValue = mailSet.parallelStream().filter(mail -> mail.getReceiver().equals(actor)).count();
            } else {
                Stream<Mail> mailStream = StreamSupport.stream(new MailSpliterator(new ArrayList<>(mailSet)), true);
                streamApiValue = mailStream.filter(mail -> mail.getReceiver().equals(actor)).count();
            }

        }
        Instant streamApiEnd = Instant.now();
        long streamApiElapsedTime = Duration.between(streamApiStart, streamApiEnd).toNanos();

        long customCollectorValue;
        Instant customCollectorApiStart = Instant.now();
        customCollectorValue = mailSet.stream().collect(new ReceiverCollector(actor));
        Instant customCollectorApiEnd = Instant.now();
        long customCollectorApiTime = Duration.between(customCollectorApiStart, customCollectorApiEnd).toNanos();

        Thread.sleep(delay);

        return new ReportContainer<>(iterationValue, streamApiValue, customCollectorValue,
            iterationElapsedTime, streamApiElapsedTime, customCollectorApiTime);
    }

}
