package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CoordinatesPack {
    @SerializedName("x")
    private final Double x;
    @SerializedName("y")
    private final Double y;
}
