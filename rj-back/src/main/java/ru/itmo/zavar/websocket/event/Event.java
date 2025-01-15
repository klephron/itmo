package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Event {
    @SerializedName("event")
    private final String event;
}
