package ru.itmo.zavar.stats.collector.status;

import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailStatus;

import java.util.HashMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class MailStatusCollector implements Collector<Mail, HashMap<MailStatus, Integer>, HashMap<MailStatus, Integer>> {

    @Override
    public Supplier<HashMap<MailStatus, Integer>> supplier() {
        return HashMap::new;
    }

    @Override
    public BiConsumer<HashMap<MailStatus, Integer>, Mail> accumulator() {
        return (mailStatusMap, mail) -> {
            mailStatusMap.put(mail.getStatus(), mailStatusMap.getOrDefault(mail.getStatus(), 0) + 1);
        };
    }

    @Override
    public BinaryOperator<HashMap<MailStatus, Integer>> combiner() {
        return (map, map2) -> {
            map.putAll(map2);
            return map;
        };
    }

    @Override
    public Function<HashMap<MailStatus, Integer>, HashMap<MailStatus, Integer>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.IDENTITY_FINISH);
    }
}
