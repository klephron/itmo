package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;

public class ErrorEvent extends Event {
    @SerializedName("args")
    private final Object args;
    @SerializedName("error")
    private final String error;

    public ErrorEvent(String event, Object args, String error) {
        super(event);
        this.args = args;
        this.error = error;
    }
}
