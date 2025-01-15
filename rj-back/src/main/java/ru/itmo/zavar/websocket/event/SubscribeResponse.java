package ru.itmo.zavar.websocket.event;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class SubscribeResponse extends Event {
    public static final String EVENT = "subscribe";

    @SerializedName("args")
    private final Args args;
    @SerializedName("status")
    private final Status status;
    @SerializedName("msg")
    private final String msg;

    public SubscribeResponse(Args args, Status status, String msg) {
        super(EVENT);
        this.args = args;
        this.status = status;
        this.msg = msg;
    }

    public SubscribeResponse(Args args, Status status) {
        this(args, status, null);
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

    public enum Status {
        @SerializedName("CREATED")
        CREATED,
        @SerializedName("RECREATED")
        RECREATED,
    }
}
