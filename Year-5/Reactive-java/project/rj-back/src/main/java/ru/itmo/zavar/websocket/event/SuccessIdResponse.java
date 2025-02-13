package ru.itmo.zavar.websocket.event;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class SuccessIdResponse extends Event {
    public static final String EVENT = "success-id";

    @SerializedName("id")
    private final Long id;

    public SuccessIdResponse(Long id) {
        super(EVENT);
        this.id = id;
    }
}
