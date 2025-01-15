package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import ru.itmo.zavar.websocket.pack.TaskPack;

@Getter
public class AddTaskMessage<T extends TaskPack> extends Message {
    public static final String OP = "actor.add_task";

    @SerializedName("args")
    private final Args<T> args;

    public AddTaskMessage(T task) {
        super(OP);
        args = new Args<T>(task);
    }

    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    @Getter
    public static class Args<T> {
        @SerializedName("task")
        T task;

        public Args(T task) {
            this.task = task;
        }

    }

    public static class Generic extends AddTaskMessage<TaskPack> {
        Generic(TaskPack task) {
            super(task);
        }
    }

    public static class MailsSendOneMessage extends AddTaskMessage<TaskPack.MailSendOne> {
        MailsSendOneMessage(TaskPack.MailSendOne task) {
            super(task);
        }
    }
}
