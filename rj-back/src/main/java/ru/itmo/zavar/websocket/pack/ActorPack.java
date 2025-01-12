package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ActorPack {
    @SerializedName("kind")
    private final Kind kind;
    @SerializedName("id")
    private final Long id;
    @SerializedName("address")
    private final AddressPack address;
    @SerializedName("tasks")
    private final List<TaskPack> tasks;

    public enum Kind {
        @SerializedName("INDIVIDUAL")
        INDIVIDUAL,
        @SerializedName("ORGANIZATION")
        ORGANIZATION,
    }
}
