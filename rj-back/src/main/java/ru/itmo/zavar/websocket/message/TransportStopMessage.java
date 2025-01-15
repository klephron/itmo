package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class TransportStopMessage extends Message {
    public static final String OP = "transport.stop";

    @SerializedName("args")
    private final Args args;

    public TransportStopMessage(Long transportId) {
        super(OP);
        this.args = new Args(transportId);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("transport_id")
        private final Long transportId;
    }
}
