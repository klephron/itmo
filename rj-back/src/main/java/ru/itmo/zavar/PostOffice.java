package ru.itmo.zavar;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import reactor.core.publisher.Mono;

@ToString
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode()
public class PostOffice {
    private final Address address;

    @EqualsAndHashCode.Exclude
    private final Map<Actor, Deque<Mail>> mailsReceived = new HashMap<>();

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

}
