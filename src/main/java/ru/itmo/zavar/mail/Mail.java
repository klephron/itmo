package ru.itmo.zavar.mail;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.itmo.zavar.actor.Actor;
import ru.itmo.zavar.mail.mark.Mark;

import java.time.LocalDateTime;
import java.util.HashSet;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public class Mail {
    private final long id;
    private final String subject;
    private final Actor sender;
    private final Actor receiver;
    private final MailData mailData;
    private final int value;
    @Setter
    private MailStatus status = MailStatus.RAW;
    private HashSet<Mark> marks;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;

    public void applyMarks(HashSet<Mark> marks) {
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
}
