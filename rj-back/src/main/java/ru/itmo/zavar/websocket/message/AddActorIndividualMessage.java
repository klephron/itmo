package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.IndividualActor;
import ru.itmo.zavar.websocket.pack.ActorPack;
import ru.itmo.zavar.websocket.pack.AddressPack;
import ru.itmo.zavar.websocket.pack.IndividualActorPack;
import ru.itmo.zavar.websocket.pack.OrganizationActorPack;

@Getter
public class AddActorIndividualMessage extends Message {
    public static final String OP = "actors.add_ind";

    @SerializedName("args")
    private final Args args;

    public AddActorIndividualMessage(IndividualActorPack actorPack) {
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
        private final IndividualActorPack actor;
    }
}
