package ru.itmo.zavar;

import ru.itmo.zavar.mail.MailGenerator;

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
        generator.generateMails(10).forEach(System.out::println);
    }
}