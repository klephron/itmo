package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.CoordinatesPack;
import ru.itmo.zavar.websocket.pack.RoutePack;

@Getter
public class AddTransportMessage extends Message {
    public static final String OP = "add-transport";

    @SerializedName("args")
    private final Args args;

    public AddTransportMessage(int capacity, double speed, double ops, String licensePlate, CoordinatesPack coordinates, RoutePack route) {
        super(OP);
        this.args = new Args(capacity, speed, ops, licensePlate, coordinates, route);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("capacity")
        private final int capacity;
        @SerializedName("speed")
        private final double speed;
        @SerializedName("ops")
        private final double ops;
        @SerializedName("license_plate")
        private final String licensePlate;
        @SerializedName("coordinates")
        private final CoordinatesPack coordinates;
        @SerializedName("route")
        private final RoutePack route;
    }
}
