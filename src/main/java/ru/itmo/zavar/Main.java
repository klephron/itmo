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

import java.util.Set;

public final class Main {
    public static void main(String[] args) {

        final int[] count = new int[]{5000, 50000, 250000};

        final MailGenerator generator = new MailGenerator();
        final MailStatistics stats = new MailStatistics();
        final MailStatus statusToCheck = MailStatus.RAW;
        final Mark markToCheck = new Mark(MarkNominal.N500, "Canada", "N500_Canada");
        final IndividualActor actorToCheck = new IndividualActor("Alexander", "Stepanov", "Pavlovich",
                new Address("Russia", "Moscow", "Moscow Oblast", "412 Walnut St", "19984"));

        for (int c : count) {
            Set<Mail> mailSet = generator.generateMails(c);
            ReportContainer<Long> statusReport = stats.reportAvailableByStatus(statusToCheck, mailSet);
            ReportContainer<Long> markReport = stats.reportAvailableByMark(markToCheck, mailSet);
            ReportContainer<Long> senderReport = stats.reportBySenderActor(actorToCheck, mailSet);
            ReportContainer<Long> receiverReport = stats.reportByReceiverActor(actorToCheck, mailSet);
            System.out.printf("Report for %d mails:\n", c);
            System.out.printf("\tCount by status %s:\n", statusToCheck.name());
            printReport(statusReport);
            System.out.printf("\tCount by mark %s:\n", markToCheck.name());
            printReport(markReport);
            System.out.printf("\tCount by sender %s %s %s:\n", actorToCheck.getName(), actorToCheck.getSurname(), actorToCheck.getPatronymic());
            printReport(senderReport);
            System.out.printf("\tCount by receiver %s %s %s:\n", actorToCheck.getName(), actorToCheck.getSurname(), actorToCheck.getPatronymic());
            printReport(receiverReport);
        }
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