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
import reactor.core.publisher.Flux;
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
    private final Coordinates coordinates;

    private final Map<Actor, Deque<Mail>> terminalMails = new HashMap<>();
    private final Map<Address, Deque<Mail>> intermediateMails = new HashMap<>();

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

    public void applyMailMono(Mono<Mail> mono) {
        mono.subscribe(m -> {
            Deque<Mail> actorMails = terminalMails.computeIfAbsent(m.getReceiver(), k -> new ArrayDeque<>());
            actorMails.push(m);
        });
    }

    public void applyMailFlux(Flux<Mail> flux) {
        flux.subscribe(m -> {
            Deque<Mail> actorMails = terminalMails.computeIfAbsent(m.getReceiver(), k -> new ArrayDeque<>());
            actorMails.push(m);
        });
    }

    // NOTE: deque can be empty, because if trying to delete value (deque)
    // there can occur desyncs while sending and receiving

    // 1 or error
    public Mono<Mail> receiveMailOne(Actor receiver) {
        Deque<Mail> actorMails = terminalMails.get(receiver);
        if (actorMails == null) {
            return Mono.error(new MailException("no container for receiver has been created"));
        }
        Mail mail = actorMails.pollLast();
        if (mail == null) {
            return Mono.error(new MailException("no mail to receive"));
        }
        return Mono.just(mail);
    }

    // 0..N or error
    public Flux<Mail> receiveMailFlux(Actor receiver) {
        Deque<Mail> actorMails = terminalMails.get(receiver);
        if (actorMails == null) {
            return Flux.error(new MailException("no container for receiver has been created"));
        }
        return Flux.generate(sink -> {
            Mail mail = actorMails.pollLast();
            if (mail == null) {
                sink.complete();
                return;
            }
            // logger.fine("received " + mail.toString());
            sink.next(mail);
        });
    }

    private enum Status {
        OPENED,
        CLOSED
    }
}
