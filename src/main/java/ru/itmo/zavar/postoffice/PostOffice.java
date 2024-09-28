package ru.itmo.zavar.postoffice;

import ru.itmo.zavar.exception.MailApplyFailedException;
import ru.itmo.zavar.exception.MarkIsNotExistsException;
import ru.itmo.zavar.exception.MarksIsOverException;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailStatus;
import ru.itmo.zavar.mail.mark.Mark;
import ru.itmo.zavar.mail.mark.MarkNominal;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class PostOffice {
    protected final HashMap<Mark, Long> marksStorage = new HashMap<>();
    protected final ArrayDeque<Mail> mailsToSend = new ArrayDeque<>();
    protected final ArrayDeque<Mail> mailsReceived = new ArrayDeque<>();
    protected final ArrayDeque<Mail> mailsIntermediary = new ArrayDeque<>();

    public void applyMail(Mail mail) throws MailApplyFailedException {
        int value = mail.getValue();

        Map<Mark, Long> result = new HashMap<>();

        List<Integer> sortedNominals = MarkNominal.toValuesList();

        for (Integer nominalValue : sortedNominals) {
            for (Map.Entry<Mark, Long> entry : marksStorage.entrySet()) {
                Mark mark = entry.getKey();
                long availableCount = entry.getValue();
                if (mark.markNominal().getValue() == nominalValue && availableCount > 0) {
                    long maxPossible = value / nominalValue;
                    long countToUse = Math.min(maxPossible, availableCount);

                    if (countToUse > 0) {
                        result.put(mark, countToUse);
                        value -= (int) (countToUse * nominalValue);
                    }
                    if (value == 0) {
                        break;
                    }
                }
            }
        }

        if (value > 0) {
            throw new MailApplyFailedException("Not enough marks to cover the value of given letter");
        } else {
            result.forEach(this::extractMark);
        }

        mail.setStatus(MailStatus.CREATED);
        mail.setCreatedAt(LocalDateTime.now());
        mailsToSend.add(mail);
    }

    public void applyAllMails(Set<Mail> mails) {
        mails.forEach(this::applyMail);
    }

    public void putMarks(HashMap<Mark, Long> marks) {
        marksStorage.putAll(marks);
    }

    private void extractMark(Mark mark, long countToExtract) throws MarkIsNotExistsException, MarksIsOverException {
        if (marksStorage.containsKey(mark)) {
            Long count = marksStorage.get(mark);
            if (count > 0) {
                marksStorage.put(mark, count - countToExtract);
            } else {
                throw new MarksIsOverException(mark.name().concat(" mark is over"));
            }
        } else {
            throw new MarkIsNotExistsException(mark.name().concat(" mark is not exists"));
        }
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
