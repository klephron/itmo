package ru.itmo.zavar;

import ru.itmo.zavar.actor.Address;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailGenerator;
import ru.itmo.zavar.mail.MailStatus;
import ru.itmo.zavar.mail.mark.Mark;
import ru.itmo.zavar.mail.mark.MarkNominal;
import ru.itmo.zavar.stats.MailStatistics;
import ru.itmo.zavar.stats.data.ReportContainer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Main {
    public static void main(String[] args) throws IOException {

        final int[] count = new int[]{5000, 15000, 25000, 50000, 100000, 250000};
        final int[] iterations = new int[]{20, 20, 15, 10, 5, 3};

        final MailGenerator generator = new MailGenerator();
        final MailStatistics stats = new MailStatistics();
        final MailStatus statusToCheck = MailStatus.RAW;
        final Mark markToCheck = new Mark(MarkNominal.N500, "Canada", "N500_Canada");

        final IndividualActor actorToCheck = new IndividualActor("Alexander", "Stepanov", "Pavlovich",
            new Address("Russia", "Moscow", "Moscow Oblast", "412 Walnut St", "19984"));

        final ExecutorService executorService = Executors.newCachedThreadPool();

        for (int j = 0; j < count.length; j++) {
            int finalJ = j;
            executorService.submit(() -> {
                System.out.printf("Thread %d for %d mails started\n", finalJ, count[finalJ]);
                for (int i = 0; i < iterations[finalJ]; i++) {
                    Set<Mail> mailSet = generator.generateMails(count[finalJ]);
                    ReportContainer<Long> statusReport = stats.reportAvailableByStatus(statusToCheck, mailSet);
                    ReportContainer<Long> markReport = stats.reportAvailableByMark(markToCheck, mailSet);
                    ReportContainer<Long> senderReport = stats.reportBySenderActor(actorToCheck, mailSet);
                    ReportContainer<Long> receiverReport = stats.reportByReceiverActor(actorToCheck, mailSet);
                    System.out.printf("Report %d for %d mails:\n", i, count[finalJ]);
                    System.out.printf("\tCount by status %s:\n", statusToCheck.name());
                    printReport(statusReport);
                    System.out.printf("\tCount by mark %s:\n", markToCheck.name());
                    printReport(markReport);
                    System.out.printf("\tCount by sender %s %s %s:\n", actorToCheck.getName(), actorToCheck.getSurname(), actorToCheck.getPatronymic());
                    printReport(senderReport);
                    System.out.printf("\tCount by receiver %s %s %s:\n", actorToCheck.getName(), actorToCheck.getSurname(), actorToCheck.getPatronymic());
                    printReport(receiverReport);
                    try {
                        toFile(statusReport, i, count[finalJ], "Status");
                        toFile(markReport, i, count[finalJ], "Mark");
                        toFile(senderReport, i, count[finalJ], "Sender");
                        toFile(receiverReport, i, count[finalJ], "Receiver");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (finalJ == count.length - 1) {
                    executorService.shutdownNow();
                    executorService.close();
                    System.exit(0);
                }
            });
        }
    }

    private static void toFile(ReportContainer<Long> container, int i, int count, String filename) throws IOException {
        Path dir = Path.of("C:\\rjdata\\").resolve(String.valueOf(count));
        if (Files.notExists(dir))
            Files.createDirectories(dir);
        if (Files.notExists(dir.resolve(filename)))
            Files.createFile(dir.resolve(filename));
        BufferedWriter bufferedWriter = Files.newBufferedWriter(dir.resolve(filename), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        bufferedWriter.append("%d,%d,%d,%d,%d,%d,%d".formatted(i, container.iterationValue(), container.iterationElapsedTime(),
            container.streamApiValue(), container.streamApiElapsedTime(), container.customCollectorValue(), container.customCollectorElapsedTime()));
        bufferedWriter.newLine();
        bufferedWriter.close();
    }

    private static void printReport(ReportContainer<Long> report) {
        System.out.print("\t\tIteration:\n");
        System.out.printf("\t\t\tCount: %d\n", report.iterationValue());
        System.out.printf("\t\t\tElapsed time: %d\n", report.iterationElapsedTime());

        System.out.print("\t\tStreamAPI:\n");
        System.out.printf("\t\t\tCount: %d\n", report.streamApiValue());
        System.out.printf("\t\t\tElapsed time: %d\n", report.streamApiElapsedTime());

        System.out.print("\t\tCustom collector:\n");
        System.out.printf("\t\t\tCount: %d\n", report.customCollectorValue());
        System.out.printf("\t\t\tElapsed time: %d\n", report.customCollectorElapsedTime());
    }
}
