package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.ActorPack;

@Getter
public class AddActorMessage extends Message {
    public static final String OP = "actors.add";

    @SerializedName("args")
    private final Args args;

    public AddActorMessage(ActorPack actorPack) {
        super(OP);
        this.args = new Args(actorPack);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("actor")
        private final ActorPack actor;
    }
}
