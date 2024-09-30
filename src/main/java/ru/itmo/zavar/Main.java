package ru.itmo.zavar;

import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailGenerator;
import ru.itmo.zavar.stats.MailStatistics;
import ru.itmo.zavar.mail.MailStatus;
import ru.itmo.zavar.stats.data.ReportContainer;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
//        PostOffice po = new PostOffice();
//        HashMap<Mark, Long> marks = new HashMap<>();
//        marks.put(new Mark(MarkNominal.N50, "", "N50"), 10L);
//        marks.put(new Mark(MarkNominal.N20, "", "N20"), 20L);
//        marks.put(new Mark(MarkNominal.N100, "", "N100"), 20L);
//        marks.put(new Mark(MarkNominal.N500, "", "N500"), 0L);
//        po.putMarks(marks);
//        po.applyMail(null);
        MailGenerator generator = new MailGenerator();
        Set<Mail> mailSet = generator.generateMails(100);
        mailSet.forEach(System.out::println);
        MailStatistics stats = new MailStatistics();
        ReportContainer<Long> longReportContainer = stats.reportAvailableByStatus(MailStatus.RAW, mailSet);
        System.out.println(longReportContainer);
    }
}