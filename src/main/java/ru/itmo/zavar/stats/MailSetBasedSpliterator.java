package ru.itmo.zavar.stats;

import ru.itmo.zavar.mail.Mail;

import java.util.*;
import java.util.function.Consumer;

public final class MailSetBasedSpliterator implements Spliterator<Mail> {
    private final Set<Mail> mailSet;
    private final Iterator<Mail> iterator;
    private long estimateSize;
    private final int batchSize;

    public MailSetBasedSpliterator(Set<Mail> mailSet, int batchSize) {
        this.mailSet = mailSet;
        this.iterator = mailSet.iterator();
        this.estimateSize = mailSet.size();
        this.batchSize = batchSize;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Mail> action) {
        Objects.requireNonNull(action);
        if (iterator.hasNext()) {
            Mail mail = iterator.next();
            action.accept(mail);
            estimateSize--;
            return true;
        }
        return false;
    }

    @Override
    public Spliterator<Mail> trySplit() {
        if (estimateSize <= batchSize) {
            return null;
        }

        Set<Mail> splitSet = new HashSet<>(batchSize);
        int count = 0;

        while (iterator.hasNext() && count < batchSize) {
            splitSet.add(iterator.next());
            count++;
            estimateSize--;
        }

        if (splitSet.isEmpty()) {
            return null;
        }

        return new MailSetBasedSpliterator(splitSet, batchSize);
    }

    @Override
    public long estimateSize() {
        return estimateSize;
    }

    @Override
    public int characteristics() {
        int characteristics = Spliterator.NONNULL | Spliterator.SIZED | Spliterator.SUBSIZED;

        if (this.mailSet instanceof SortedSet) {
            characteristics |= Spliterator.ORDERED | Spliterator.SORTED;
        } else if (this.mailSet instanceof LinkedHashSet) {
            characteristics |= Spliterator.ORDERED;
        }

        characteristics |= Spliterator.IMMUTABLE;

        return characteristics;
    }
}
