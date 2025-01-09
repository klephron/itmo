package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;

public class ErrorEvent extends Event {
    @SerializedName("data")
    private final Object data;
    @SerializedName("msg")
    private final String msg;

    public ErrorEvent(Object data, String msg) {
        super("error");
        this.data = data;
        this.msg = msg;
    }
}
