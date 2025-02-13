package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;

public class WarningResponse extends Event {
    public static final String EVENT = "warning";

    @SerializedName("msg")
    private final String msg;

    public WarningResponse(String msg) {
        super(EVENT);
        this.msg = msg;
    }
}
