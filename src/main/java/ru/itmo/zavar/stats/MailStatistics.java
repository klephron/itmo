package ru.itmo.zavar.stats;

import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailStatus;
import ru.itmo.zavar.stats.collector.MailStatusCollector;
import ru.itmo.zavar.stats.data.ReportContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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

}
