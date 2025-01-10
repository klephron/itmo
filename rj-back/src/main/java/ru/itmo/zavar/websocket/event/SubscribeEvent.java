package ru.itmo.zavar.websocket.event;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.ActorPack;
import ru.itmo.zavar.websocket.pack.PostOfficePack;
import ru.itmo.zavar.websocket.pack.TransportPack;

public class SubscribeEvent extends Event {
    public static final String EVENT = "subscribe";

    public SubscribeEvent(Args args) {
        super(EVENT);
    }

    @RequiredArgsConstructor
    @Getter
    public static class Args {
        @SerializedName("session_id")
        private final String sessionId;
        @SerializedName("post_offices")
        private final List<PostOfficePack> postoffices;
        @SerializedName("transports")
        private final List<TransportPack> transports;
        @SerializedName("actors")
        private final List<ActorPack> actors;
    }
}
