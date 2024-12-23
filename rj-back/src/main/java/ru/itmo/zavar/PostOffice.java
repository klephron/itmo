package ru.itmo.zavar;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.itmo.zavar.Mail.Mark;

public class PostOffice extends Coordinates {
    protected final HashMap<Mark, Long> marksStorage = new HashMap<>();
    protected final ArrayDeque<Mail> mailsToSend = new ArrayDeque<>();
    protected final ArrayDeque<Mail> mailsReceived = new ArrayDeque<>();
    protected final ArrayDeque<Mail> mailsIntermediary = new ArrayDeque<>();
    private final List<Integer> sortedNominals = Mark.Nominal.toValuesList();

    public void applyMail(Mail mail) throws MailException {
        long value = mail.getValue();

        Map<Mark, Long> result = new HashMap<>();

        boolean b = tryToApplyMarks(value, result);
        if (!b) {
            throw new MailException("Not enough marks to cover the value of given letter");
        } else {
            result.forEach(this::extractMark);
        }

        mail.setStatus(Mail.Status.CREATED);
        mail.setCreatedAt(LocalDateTime.now());
        mailsToSend.add(mail);
    }

    private boolean tryToApplyMarks(long value, Map<Mark, Long> result) {
        if (value == 0) {
            return true;
        }
        for (Integer nominalValue : sortedNominals) {
            for (Map.Entry<Mark, Long> entry : marksStorage.entrySet()) {
                Mark mark = entry.getKey();
                long availableCount = entry.getValue();
                if (mark.markNominal().getValue() == nominalValue && availableCount > 0) {
                    long maxPossible = value / nominalValue;
                    long countToUse = Math.min(maxPossible, availableCount);

                    for (long i = countToUse; i > 0; i--) {
                        result.put(mark, result.getOrDefault(mark, 0L) + i);
                        long newValue = value - i * nominalValue;

                        if (tryToApplyMarks(newValue, result)) {
                            return true;
                        }
                        // backtracking
                        result.put(mark, result.get(mark) - i);
                    }
                }
            }
        }
        return false;
    }

    public void applyAllMails(Set<Mail> mails) {
        mails.forEach(this::applyMail);
    }

    public void putMarks(HashMap<Mark, Long> marks) {
        marksStorage.putAll(marks);
    }

    private void extractMark(Mark mark, long countToExtract) throws MarkException {
        if (marksStorage.containsKey(mark)) {
            Long count = marksStorage.get(mark);
            if (count > 0) {
                marksStorage.put(mark, count - countToExtract);
            } else {
                throw new MarkException(mark.name().concat(" mark is over"));
            }
        } else {
            throw new MarkException(mark.name().concat(" mark does not exist"));
        }
    }
}
