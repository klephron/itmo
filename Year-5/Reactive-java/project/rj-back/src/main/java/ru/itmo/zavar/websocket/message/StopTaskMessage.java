package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.ActorPack;

@Getter
public class StopTaskMessage extends Message {
    public static final String OP = "actor.stop_task";

    @SerializedName("args")
    private final Args args;

    public StopTaskMessage(Long actorId, Long taskId) {
        super(OP);
        this.args = new Args(actorId, taskId);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("actor_id")
        private final Long actorId;
        @SerializedName("task_id")
        private final Long taskId;
    }
}
