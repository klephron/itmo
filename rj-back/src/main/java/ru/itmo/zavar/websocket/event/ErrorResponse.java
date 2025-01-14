package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse extends Event {
    public static final String EVENT = "error";

    @SerializedName("data")
    private final Object data;
    @SerializedName("msg")
    private final String msg;

    public ErrorResponse(Object data, String msg) {
        super(EVENT);
        this.data = data;
        this.msg = msg;
    }
}
