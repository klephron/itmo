package ru.itmo.zavar;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
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
    private Mail.Status status = Mail.Status.RAW;
    private ArrayList<Mark> marks;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;

    public void applyMarks(ArrayList<Mark> marks) {
        if (this.marks == null && marks != null) {
            this.marks = marks;
        } else {
            throw new RuntimeException("Marks is already set");
        }
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (this.createdAt == null && createdAt != null) {
            this.createdAt = createdAt;
        } else {
            throw new RuntimeException("Creation date is already set");
        }
    }

    public void setSentAt(LocalDateTime sentAt) {
        if (this.sentAt == null && sentAt != null) {
            this.sentAt = sentAt;
        } else {
            throw new RuntimeException("Sent date is already set");
        }
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        if (this.receivedAt == null && receivedAt != null) {
            this.receivedAt = receivedAt;
        } else {
            throw new RuntimeException("Received date is already set");
        }
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
        RAW,
        CREATED,
        SENT,
        RECEIVED
    }
}
