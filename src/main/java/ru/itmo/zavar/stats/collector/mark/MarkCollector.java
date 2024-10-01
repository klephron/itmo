package ru.itmo.zavar.stats.collector.mark;

import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.mark.Mark;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@RequiredArgsConstructor
public final class MarkCollector implements Collector<Mail, Long[], MarkCollectorReport> {

    private final Mark mark;

    @Override
    public Supplier<Long[]> supplier() {
        return () -> new Long[]{0L, 0L};
    }

    @Override
    public BiConsumer<Long[], Mail> accumulator() {
        return (acc, mail) -> {
            if (mail.getMarks().contains(mark)) {
                acc[0] = acc[0] + 1;
                acc[1] = acc[1] + mail.getMarks().stream().filter(m -> m.equals(mark)).count();
            }
        };
    }

    @Override
    public BinaryOperator<Long[]> combiner() {
        return (acc, acc2) -> {
            acc[0] += acc2[0];
            acc[1] += acc2[1];
            return acc;
        };
    }

    @Override
    public Function<Long[], MarkCollectorReport> finisher() {
        return acc -> new MarkCollectorReport(acc[0], acc[1]);
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }
}
