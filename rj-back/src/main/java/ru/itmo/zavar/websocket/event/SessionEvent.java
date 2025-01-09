package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.Session;

@Getter
public class SessionEvent extends Event {
    @SerializedName("args")
    private final Args args;

    public SessionEvent(Session session, String status) {
        super("session");
        this.args = new Args(session.getChannelId().asLongText(), status);
    }

    @RequiredArgsConstructor
    @Getter
    public class Args {
        @SerializedName("session_id")
        private final String sessionId;
        @SerializedName("status")
        private final String status;
    }
}
