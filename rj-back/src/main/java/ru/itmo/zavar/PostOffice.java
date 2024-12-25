package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@ToString
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PostOffice {
    @EqualsAndHashCode.Include
    private final Address address;

    private final Map<Actor, Deque<Mail>> mailsReceived = new HashMap<>();
    private final Sinks.Many<Transport> transportSink = Sinks.many().unicast().onBackpressureBuffer();
    private Disposable transportSinkSubscription = null;
    private Status transportStatus = Status.CLOSED;

    public void open() {
        if (transportStatus != Status.CLOSED) {
            return;
        }
        transportSinkSubscription = transportSink.asFlux()
                .subscribeOn(Schedulers.single())
                .subscribe(transport -> {
                    logger.fine("in " + this.toString() + " handling " + transport.toString());
                });
        transportStatus = Status.OPENED;
    }

    public void close() {
        if (transportStatus != Status.OPENED) {
            return;
        }
        transportSinkSubscription.dispose();
        transportSinkSubscription = null;
        transportStatus = Status.CLOSED;
    }

    public void applyMailOne(Mono<Mail> mail) {
        mail.subscribe(m -> {
            Deque<Mail> actorMails = mailsReceived.computeIfAbsent(m.getReceiver(), k -> new ArrayDeque<>());
            actorMails.push(m);
        });
    }

    public Mono<Mail> receiveMailOne(Actor receiver) {
        Deque<Mail> actorMails = mailsReceived.computeIfAbsent(receiver, k -> new ArrayDeque<>());
        Mail mail = actorMails.poll();
        if (mail == null) {
            return Mono.error(new MailException("no mail to receive"));
        }
        return Mono.just(mail);
    }

    private enum Status {
        OPENED,
        CLOSED
    }
}
