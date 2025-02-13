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
        public Generic(TaskPack task) {
            super(task);
        }
    }

    public static class SendMailOneMessage extends AddTaskMessage<TaskPack.SendMailOne> {
        public SendMailOneMessage(TaskPack.SendMailOne task) {
            super(task);
        }
    }

    public static class SendMailRepeatedMessage extends AddTaskMessage<TaskPack.SendMailRepeated> {
        public SendMailRepeatedMessage(TaskPack.SendMailRepeated task) {
            super(task);
        }
    }

    public static class ReceiveMailOneMessage extends AddTaskMessage<TaskPack.ReceiveMailOne> {
        public ReceiveMailOneMessage(TaskPack.ReceiveMailOne task) {
            super(task);
        }
    }

    public static class ReceiveMailManyMessage extends AddTaskMessage<TaskPack.ReceiveMailMany> {
        public ReceiveMailManyMessage(TaskPack.ReceiveMailMany task) {
            super(task);
        }
    }
}
