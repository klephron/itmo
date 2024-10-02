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

public final class Main {
    public static void main(String[] args) throws IOException {

        final int[] count = new int[]{50000};

        final MailGenerator generator = new MailGenerator();
        final MailStatistics stats = new MailStatistics();
        final MailStatus statusToCheck = MailStatus.RAW;
        final Mark markToCheck = new Mark(MarkNominal.N500, "Canada", "N500_Canada");
        final IndividualActor actorToCheck = new IndividualActor("Alexander", "Stepanov", "Pavlovich",
            new Address("Russia", "Moscow", "Moscow Oblast", "412 Walnut St", "19984"));

        for (int c : count) {
            for (int i = 0; i < 2; i++) {
                Set<Mail> mailSet = generator.generateMails(c);
                ReportContainer<Long> statusReport = stats.reportAvailableByStatus(statusToCheck, mailSet);
                ReportContainer<Long> markReport = stats.reportAvailableByMark(markToCheck, mailSet);
                ReportContainer<Long> senderReport = stats.reportBySenderActor(actorToCheck, mailSet);
                ReportContainer<Long> receiverReport = stats.reportByReceiverActor(actorToCheck, mailSet);
                System.out.printf("Report %d for %d mails:\n", i, c);
                System.out.printf("\tCount by status %s:\n", statusToCheck.name());
                printReport(statusReport);
                System.out.printf("\tCount by mark %s:\n", markToCheck.name());
                printReport(markReport);
                System.out.printf("\tCount by sender %s %s %s:\n", actorToCheck.getName(), actorToCheck.getSurname(), actorToCheck.getPatronymic());
                printReport(senderReport);
                System.out.printf("\tCount by receiver %s %s %s:\n", actorToCheck.getName(), actorToCheck.getSurname(), actorToCheck.getPatronymic());
                printReport(receiverReport);
                toFile(statusReport, i, c, "Status");
                toFile(markReport, i, c, "Mark");
                toFile(senderReport, i, c, "Sender");
                toFile(receiverReport, i, c, "Receiver");
            }
        }
    }

    private static void toFile(ReportContainer<Long> container, int i, int count, String filename) throws IOException {
        Path dir = Path.of("C:\\rjdata\\").resolve(String.valueOf(count));
        if (Files.notExists(dir))
            Files.createDirectories(dir);
        if (Files.notExists(dir.resolve(filename)))
            Files.createFile(dir.resolve(filename));
        BufferedWriter bufferedWriter = Files.newBufferedWriter(dir.resolve(filename), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        bufferedWriter.append(new String("%d,%d,%d,%d,%d,%d,%d").formatted(i, container.iterationValue(), container.iterationElapsedTime(),
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
