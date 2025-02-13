package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
public class SubscribeMessage extends Message {
    public static final String OP = "subscribe";

    @SerializedName("args")
    private final Args args;

    public SubscribeMessage(Float ups, List<Long> postOfficeIds, List<Long> transportIds, List<Long> actorIds) {
        super(OP);
        this.args = new Args(ups, postOfficeIds, transportIds, actorIds);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        // updates per second
        @SerializedName("ups")
        private final Float ups;
        @SerializedName("postoffice_ids")
        private final List<Long> postofficeIds;
        @SerializedName("transport_ids")
        private final List<Long> transportIds;
        @SerializedName("actor_ids")
        private final List<Long> actorIds;
    }
}
