package ru.itmo.zavar.postoffice;

import ru.itmo.zavar.exception.MarkIsNotExistsException;
import ru.itmo.zavar.exception.MarksIsOverException;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.mark.Mark;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class PostOffice {
    protected final HashMap<Mark, Long> marksStorage = new HashMap<>();
    protected final ArrayDeque<Mail> mailsToSend = new ArrayDeque<>();
    protected final ArrayDeque<Mail> mailsReceived = new ArrayDeque<>();
    protected final ArrayDeque<Mail> mailsIntermediary = new ArrayDeque<>();

    public void applyMail(Mail mail) { //TODO exceptions
        int value = 867; //mail.getValue();

        List<Mark> marksByValueList = marksStorage.keySet().stream().sorted(Comparator.comparingInt(o -> -o.markNominal().getValue())).toList();
        List<Integer> result = new ArrayList<>();

        for (Mark markEntry : marksByValueList) {
            while (value >= markEntry.markNominal().getValue()) {
                extractMark(markEntry);
                value -= markEntry.markNominal().getValue();
                result.add(markEntry.markNominal().getValue());
            }
        }
        result.forEach(System.out::println);
    }

    public void applyAllMails(Set<Mail> mails) {
        mails.forEach(this::applyMail);
    }

    public void putMarks(HashMap<Mark, Long> marks) {
        marksStorage.putAll(marks);
    }

    private void extractMark(Mark mark) throws MarkIsNotExistsException, MarksIsOverException {
        if (marksStorage.containsKey(mark)) {
            Long count = marksStorage.get(mark);
            if (count > 0) {
                marksStorage.put(mark, count - 1);
            } else {
                throw new MarksIsOverException(mark.name().concat(" mark is over"));
            }
        } else {
            throw new MarkIsNotExistsException(mark.name().concat(" mark is not exists"));
        }
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor)
    {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
