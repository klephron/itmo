package ru.itmo.zavar.model;

import lombok.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;
import ru.itmo.zavar.exception.MailException;
import ru.itmo.zavar.exception.PostofficeException;
import ru.itmo.zavar.model.actor.Actor;
import ru.itmo.zavar.model.address.Address;
import ru.itmo.zavar.model.address.PostalAddress;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static ru.itmo.zavar.model.World.logger;

@ToString
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Postoffice {
    @EqualsAndHashCode.Include
    @Setter
    private Long id;
    private final Address address;
    private final Coordinates coordinates;

    private final Map<Actor, Deque<Mail>> terminalMails = new HashMap<>();
    private final Map<PostalAddress, Deque<Mail>> intermediateMails = new HashMap<>();

    private final Sinks.Many<ImmutablePair<Transport, Sinks.One<Void>>> transportSink = Sinks.many().unicast()
            .onBackpressureBuffer();
    private Disposable transportSinkSubscription = null;

    // statistics
    private long terminalMailsCount = 0;
    private long intermediateMailsCount = 0;
    private long transportsQueued = 0;
    private long transportsProcessed = 0;

    public void open() {
        if (isTransportOpen()) {
            return;
        }
        transportSinkSubscription = transportSink.asFlux()
                .flatMapSequential(pair -> Mono.fromCallable(() -> processTransport(pair.left, pair.right))
                        .flatMap(delay -> Mono.delay(delay).thenReturn(delay)))
                .subscribe();
    }

    public void close() {
        if (!isTransportOpen()) {
            return;
        }
        transportSinkSubscription.dispose();
        transportSinkSubscription = null;
        transportsQueued = 0;
        transportsProcessed = 0;
    }

    // NOTE: there are different implementations of it:
    // 1. returning flux of mails to receive (moving messages between each other)
    // 2. returning signal of completion (like doing yourself and signalling)

    // returns signal of process completion
    public Mono<Void> transportDeliver(Transport transport) {
        if (!isTransportOpen()) {
            return Mono.error(new PostofficeException("post office is closed for transport processing"));
        }
        Sinks.One<Void> sink = Sinks.one();
        transportSink.emitNext(new ImmutablePair<Transport, Sinks.One<Void>>(transport, sink),
                EmitFailureHandler.FAIL_FAST);
        ++transportsQueued;
        return sink.asMono();
    }

    public void applyMailMono(Mono<Mail> mono) {
        mono.subscribe(this::applyMail);
    }

    public void applyMailFlux(Flux<Mail> flux) {
        flux.subscribe(this::applyMail);
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
        --terminalMailsCount;
        mail.setReceivedAt(LocalDateTime.now());
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
            --terminalMailsCount;
            mail.setReceivedAt(LocalDateTime.now());
            sink.next(mail);
        });
    }

    // TODO: handle marks
    private void applyMail(Mail mail) {
        mail.setAppliedAt(LocalDateTime.now());
        PostalAddress receiverAddress = mail.getReceiver().getAddress().postalAddress();
        if (receiverAddress.equals(this.getAddress().postalAddress())) {
            Deque<Mail> actorMails = terminalMails.computeIfAbsent(mail.getReceiver(), k -> new ArrayDeque<>());
            actorMails.push(mail);
            ++terminalMailsCount;
            logger.info("Mail added to terminal queue (" + mail.getId() + ")");
        } else {
            Deque<Mail> addressMails = intermediateMails.computeIfAbsent(receiverAddress, k -> new ArrayDeque<>());
            addressMails.push(mail);
            ++intermediateMailsCount;
            logger.info("Mail added to intermediate queue (" + mail.getId() + ")");
        }
    }

    private boolean isTransportOpen() {
        return transportSinkSubscription != null;
    }

    // returns process time
    private Duration processTransport(Transport transport, Sinks.One<Void> signal) {
        long startNanos = System.nanoTime();
        int operations = 0;

        logger.fine("transport " + transport.getLicensePlate() + " with delivery " + transport.getDeliveryAddresses()
                + " and loading " + transport.getLoadingAddresses() + " visited post office "
                + this.getAddress().postalAddress());

        // deliver from transport
        Iterator<Mail> iterator = transport.getMails().iterator();
        while (iterator.hasNext()) {
            Mail mail = iterator.next();
            PostalAddress receiverAddress = mail.getReceiver().getAddress().postalAddress();

            if (transport.getDeliveryAddresses().contains(receiverAddress)) {
                if (receiverAddress.equals(this.getAddress().postalAddress())) {
                    Deque<Mail> actorMails = terminalMails.computeIfAbsent(mail.getReceiver(), k -> new ArrayDeque<>());
                    actorMails.push(mail);
                    ++terminalMailsCount;
                } else {
                    Deque<Mail> addressMails = intermediateMails.computeIfAbsent(receiverAddress,
                            k -> new ArrayDeque<>());
                    addressMails.push(mail);
                    ++intermediateMailsCount;
                }
                iterator.remove();
                logger.fine("transport " + transport.getLicensePlate() + " delivered mail " + mail.getId() + ", "
                        + mail.getSubject() + " to post office" + this.getAddress().postalAddress());
                operations += 1;
            }
        }

        // load transport
        PriorityQueue<Deque<Mail>> postalBuckets = new PriorityQueue<Deque<Mail>>(new DequeMailComparator());
        postalBuckets.addAll(
                transport.getLoadingAddresses().stream().map(intermediateMails::get).filter(Objects::nonNull)
                        .toList());

        int mxCount = transport.getCapacity() - transport.getMails().size();
        for (int i = 0; i < mxCount; ++i) {
            Deque<Mail> bucket = postalBuckets.poll();
            if (bucket == null || bucket.isEmpty()) {
                break;
            }
            Mail mail = bucket.pollLast();
            mail.setSentAt(LocalDateTime.now());
            transport.getMails().addLast(mail);
            operations += 1;
            --intermediateMailsCount;
            logger.fine("transport " + transport.getLicensePlate() + " loaded mail " + mail.getId() + ", "
                    + mail.getSubject() + " from post office" + this.getAddress().postalAddress());
            if (!bucket.isEmpty()) {
                postalBuckets.add(bucket);
            }
        }

        signal.emitEmpty(EmitFailureHandler.FAIL_FAST);
        ++transportsProcessed;

        long delayNanos = (long) ((double) operations / transport.getOps() * 1_000_000_000)
                - (System.nanoTime() - startNanos);
        var delay = Duration.ofNanos(delayNanos < 0 ? 0 : delayNanos);
        logger.fine("delayed by " + delay);

        return delay;
    }

    private class DequeMailComparator implements Comparator<Deque<Mail>> {
        @Override
        public int compare(Deque<Mail> lsv, Deque<Mail> rsv) {
            if (lsv.isEmpty() && rsv.isEmpty()) {
                return 0;
            }
            if (lsv.isEmpty()) {
                return 1;
            }
            if (rsv.isEmpty()) {
                return -1;
            }
            Mail lsvMail = lsv.peekLast();
            Mail rsvMail = rsv.peekLast();
            return lsvMail.getAppliedAt().compareTo(rsvMail.getAppliedAt());
        }

    }
}
