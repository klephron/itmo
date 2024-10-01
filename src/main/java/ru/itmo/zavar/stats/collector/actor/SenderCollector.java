package ru.itmo.zavar.stats.collector.actor;

import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.Mail;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@RequiredArgsConstructor
public final class SenderCollector implements Collector<Mail, Long[], Long> {

    private final IndividualActor actor;

    @Override
    public Supplier<Long[]> supplier() {
        return () -> new Long[] {0L};
    }

    @Override
    public BiConsumer<Long[], Mail> accumulator() {
        return (acc, mail) -> {
            if (mail.getSender().equals(actor)) {
                acc[0] = acc[0] + 1;
            }
        };
    }

    @Override
    public BinaryOperator<Long[]> combiner() {
        return (acc, acc2) -> {
            acc[0] += acc2[0];
            return acc;
        };
    }

    @Override
    public Function<Long[], Long> finisher() {
        return acc -> acc[0];
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of();
    }
}
