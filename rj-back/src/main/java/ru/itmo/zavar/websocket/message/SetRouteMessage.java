package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.RoutePack;

@Getter
public class SetRouteMessage extends Message {
    public static final String OP = "transport.set_route";

    @SerializedName("args")
    private final Args args;

    public SetRouteMessage(Long transportId, RoutePack routePack) {
        super(OP);
        this.args = new Args(transportId, routePack);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("transport_id")
        private final Long transportId;
        @SerializedName("route")
        private final RoutePack route;
    }
}
