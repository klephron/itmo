package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class SessionResponse extends Event {
    public static final String EVENT = "session";

    @SerializedName("args")
    private final Args args;

    public SessionResponse(Args args) {
        super(EVENT);
        this.args = args;
    }

    @RequiredArgsConstructor
    @Getter
    public static class Args {
        @SerializedName("session_id")
        private final String sessionId;
        @SerializedName("status")
        private final Status status;
    }

    public enum Status {
        @SerializedName("CREATED")
        CREATED,
    }

}
