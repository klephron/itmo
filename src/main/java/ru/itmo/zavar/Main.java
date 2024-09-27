package ru.itmo.zavar;

import ru.itmo.zavar.mail.mark.Mark;
import ru.itmo.zavar.mail.mark.MarkNominal;
import ru.itmo.zavar.postoffice.PostOffice;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        PostOffice po = new PostOffice();
        HashMap<Mark, Long> marks = new HashMap<>();
        marks.put(new Mark(MarkNominal.N50, "", "N50"), 10L);
        marks.put(new Mark(MarkNominal.N1, "", "N1"), 20L);
        marks.put(new Mark(MarkNominal.N100, "", "N100"), 20L);
        marks.put(new Mark(MarkNominal.N500, "", "N500"), 10L);
        po.putMarks(marks);
        po.applyMail(null);
    }
}