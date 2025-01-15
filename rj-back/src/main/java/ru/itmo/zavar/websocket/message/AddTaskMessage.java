package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.TaskPack;

@Getter
public class AddTaskMessage extends Message {
    public static final String OP = "actor.add_task";

    @SerializedName("args")
    private final Args args;

    public AddTaskMessage(Long actorId, Long postOfficeId, TaskPack.Kind kind, Long interval, AddTaskMessage.Args.MailSimplePack mailSimplePack) {
        super(OP);
        args = new Args(actorId, postOfficeId, kind, interval, mailSimplePack);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("actor_id")
        private final Long actorId;
        @SerializedName("postoffice_id")
        private final Long postofficeId;
        @SerializedName("kind")
        private final TaskPack.Kind kind;
        @SerializedName("interval")
        private final Long interval;
        @SerializedName("mail")
        private final MailSimplePack mailSimplePack;

        @RequiredArgsConstructor
        @Getter
        public static class MailSimplePack {
            @SerializedName("sender_id")
            private final Long senderId;
            @SerializedName("receiver_id")
            private final Long receiverId;
            @SerializedName("subject")
            private final String subject;
            @SerializedName("text")
            private final String text;
        }
    }
}
