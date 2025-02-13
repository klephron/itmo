package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
// NOTE: generates N marks or N mails each interval (not synced)
// if interval_ms is null -> do single
public class TaskPack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("kind")
    private final Kind kind;
    @SerializedName("postoffice_id")
    private final Long postofficeId;
    @SerializedName("actor_id")
    private final Long actorId;

    public enum Kind {
        @SerializedName("SEND_MAIL_ONE")
        SEND_MAIL_ONE,
        @SerializedName("SEND_MAIL_REPEATED")
        SEND_MAIL_REPEATED,
        @SerializedName("RECEIVE_MAIL_ONE")
        RECEIVE_MAIL_ONE,
        @SerializedName("RECEIVE_MAIL_MANY")
        RECEIVE_MAIL_MANY,
        @SerializedName("SEND_MARK_REPEATED")
        SEND_MARK_REPEATED,
    }

    @Getter
    public static class SendMailOne extends TaskPack {
        @SerializedName("mail")
        private final MailPack mail;

        public SendMailOne(Long id, Long actorId, Long postOfficeId, MailPack mail) {
            super(id, Kind.SEND_MAIL_ONE, postOfficeId, actorId);
            this.mail = mail;
        }

    }

    @Getter
    public static class SendMailRepeated extends TaskPack {
        @SerializedName("interval_ms")
        private final Integer intervalMs;
        @SerializedName("mail")
        private final MailPack mail;

        public SendMailRepeated(Long id, Long actorId, Long postOfficeId, Integer intervalMs, MailPack mail) {
            super(id, Kind.SEND_MAIL_REPEATED, postOfficeId, actorId);
            this.intervalMs = intervalMs;
            this.mail = mail;
        }

    }

    @Getter
    public static class ReceiveMailOne extends TaskPack {
        public ReceiveMailOne(Long id, Long postOfficeId, Long actorId) {
            super(id, Kind.RECEIVE_MAIL_ONE, postOfficeId, actorId);
        }
    }

    @Getter
    public static class ReceiveMailMany extends TaskPack {
        public ReceiveMailMany(Long id, Long postOfficeId, Long actorId) {
            super(id, Kind.RECEIVE_MAIL_MANY, postOfficeId, actorId);
        }
    }

    @Getter
    public static class MarkSendRepeated extends TaskPack {
        @SerializedName("interval_ms")
        private final Integer intervalMs;
        @SerializedName("n")
        private final Integer n;
        @SerializedName("mark")
        private final MarkPack mark;

        public MarkSendRepeated(Long id, Kind kind, Long postOfficeId, Integer intervalMs, Integer n,
                MarkPack mark, Long actorId) {
            super(id, kind, postOfficeId, actorId);
            this.intervalMs = intervalMs;
            this.n = n;
            this.mark = mark;
        }
    }
}
