package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.RoutePack;

@Getter
public class TransportStartMessage extends Message {
    public static final String OP = "transport.start";

    @SerializedName("args")
    private final Args args;

    public TransportStartMessage(Long transportId, Integer index) {
        super(OP);
        this.args = new Args(transportId, index);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("transport_id")
        private final Long transportId;
        @SerializedName("index")
        private final Integer index;
    }
}
