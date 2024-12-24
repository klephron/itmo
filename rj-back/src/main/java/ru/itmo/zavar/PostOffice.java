package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import java.util.ArrayDeque;
import java.util.logging.Level;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import reactor.core.publisher.Mono;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@Getter
public class PostOffice {
    private final Address address;
    private final ArrayDeque<Mail> mailsToSend = new ArrayDeque<>();

    public void applyMailOne(Mono<Mail> mail) {
        mail.subscribe(m -> {
            mailsToSend.push(m);
        });
    }

}
