package ru.itmo.zavar.stats;

import ru.itmo.zavar.mail.Mail;

import java.util.*;
import java.util.function.Consumer;

public final class MailSpliterator implements Spliterator<Mail> {
    private final List<Mail> mailList;
    private int current;

    public MailSpliterator(List<Mail> mailSet) {
        this.mailList = mailSet;
        this.current = 0;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Mail> action) {
        if (current < mailList.size()) {
            action.accept(mailList.get(current++));
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<Mail> trySplit() {
        int remaining = mailList.size() - current;
        if (remaining < 2) {
            return null;
        }
        int splitSize = remaining / 2;
        Spliterator<Mail> split = new MailSpliterator(mailList.subList(current, current + splitSize));
        current += splitSize;
        return split;
    }

    @Override
    public long estimateSize() {
        return mailList.size() - current;
    }

    @Override
    public int characteristics() {
        return ORDERED | SIZED | SUBSIZED | NONNULL | IMMUTABLE;
    }
}
