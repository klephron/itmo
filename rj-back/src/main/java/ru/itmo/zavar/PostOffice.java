package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.commons.lang3.tuple.ImmutablePair;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@ToString
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PostOffice {
    @EqualsAndHashCode.Include
    private final Address address;
    private final Coordinates coordinates;

    private final Map<Actor, Deque<Mail>> terminalMails = new HashMap<>();
    private final Map<PostalAddress, Deque<Mail>> intermediateMails = new HashMap<>();

    private final Sinks.Many<ImmutablePair<Transport, Sinks.One<Void>>> transportSink = Sinks.many().unicast()
            .onBackpressureBuffer();
    private Disposable transportSinkSubscription = null;

    public void open() {
        if (isTransportOpen()) {
            return;
        }
        transportSinkSubscription = transportSink.asFlux()
                .subscribe(pair -> processTransport(pair.left, pair.right));
    }

    public void close() {
        if (!isTransportOpen()) {
            return;
        }
        transportSinkSubscription.dispose();
        transportSinkSubscription = null;
    }

    // NOTE: there are different implementations of it:
    // 1. returning flux of mails to receive (moving messages between each other)
    // 2. returning signal of completion (like doing yourself and signalling)

    // returns signal of process completion
    public Mono<Void> transportDeliver(Transport transport) {
        if (!isTransportOpen()) {
            return Mono.error(new PostOfficeException("post office is closed for transport processing"));
        }
        Sinks.One<Void> sink = Sinks.one();
        transportSink.emitNext(new ImmutablePair<Transport, Sinks.One<Void>>(transport, sink),
                EmitFailureHandler.FAIL_FAST);
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

    // TODO: handle marks
    private void applyMail(Mail mail) {
        mail.setAppliedAt(LocalDateTime.now());
        PostalAddress receiverAddress = mail.getReceiver().getAddress().postalAddress();
        if (receiverAddress.equals(this.getAddress().postalAddress())) {
            Deque<Mail> actorMails = terminalMails.computeIfAbsent(mail.getReceiver(), k -> new ArrayDeque<>());
            actorMails.push(mail);
        } else {
            Deque<Mail> addressMails = intermediateMails.computeIfAbsent(receiverAddress, k -> new ArrayDeque<>());
            addressMails.push(mail);
        }
    }

    private boolean isTransportOpen() {
        return transportSinkSubscription != null;
    }

    private void processTransport(Transport transport, Sinks.One<Void> signal) {
        logger.fine("transport " + transport.getNumber() + " with delivery " + transport.getDeliveryAddresses()
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
                } else {
                    Deque<Mail> addressMails = intermediateMails.computeIfAbsent(receiverAddress,
                            k -> new ArrayDeque<>());
                    addressMails.push(mail);
                }
                iterator.remove();
                logger.fine("transport " + transport.getNumber() + " delivered mail " + mail.getId() + ", "
                        + mail.getSubject() + " to post office" + this.getAddress().postalAddress());
            }
        }

        // load transport
        PriorityQueue<Deque<Mail>> postalBuckets = new PriorityQueue<Deque<Mail>>(new DequeMailComparator());
        postalBuckets.addAll(
                transport.getLoadingAddresses().stream().map(da -> intermediateMails.get(da)).filter(pb -> pb != null)
                        .toList());

        int mxCount = transport.getCapacity() - transport.getMails().size();
        for (int i = 0; i < mxCount; ++i) {
            Deque<Mail> bucket = postalBuckets.poll();
            if (bucket == null || bucket.isEmpty()) {
                break;
            }
            Mail mail = bucket.pollLast();
            transport.getMails().addLast(mail);
            logger.fine("transport " + transport.getNumber() + " loaded mail " + mail.getId() + ", "
                    + mail.getSubject() + " from post office" + this.getAddress().postalAddress());
            if (!bucket.isEmpty()) {
                postalBuckets.add(bucket);
            }
        }

        signal.emitEmpty(EmitFailureHandler.FAIL_FAST);
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
