package ru.itmo.zavar.model;

import lombok.*;
import ru.itmo.zavar.model.actor.Actor;

import static ru.itmo.zavar.model.World.logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class Mail {
    private final long id;
    private final String subject;
    private final Actor sender;
    private final Actor receiver;
    private final Mail.Data mailData;
    private final long value;
    @Setter
    private ArrayList<Mark> marks;

    private final LocalDateTime createdAt = LocalDateTime.now();
    @Setter
    private LocalDateTime appliedAt; // applied by post office (from sender to postoffice)
    @Setter
    private LocalDateTime sentAt; // loaded by transport
    @Setter
    private LocalDateTime receivedAt; // received by receiver

    public Status getStatus() {
        if (appliedAt == null) {
            return Status.CREATED;
        }
        if (sentAt == null) {
            return Status.APPLIED;
        }
        if (receivedAt == null) {
            return Status.SENT;
        }
        return Status.RECEIVED;
    }

    public record Mark(Mark.Nominal markNominal, String country, String name) {

        @Getter
        public enum Nominal {
            N1(1),
            N10(10),
            N20(20),
            N25(25),
            N50(50),
            N100(100),
            N150(150),
            N200(200),
            N250(250),
            N500(500);

            private final int value;

            Nominal(int value) {
                this.value = value;
            }

            public static List<Integer> toValuesList() {
                return Arrays.stream(values()).map(nominal -> nominal.value).sorted(Comparator.reverseOrder()).toList();
            }
        }
    }

    public static record Data(String text) {
    }

    public enum Status {
        CREATED,
        APPLIED,
        SENT,
        RECEIVED
    }
}
