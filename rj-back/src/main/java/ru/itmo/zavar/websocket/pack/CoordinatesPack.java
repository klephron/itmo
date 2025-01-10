package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CoordinatesPack {
    @SerializedName("x")
    private final double x;
    @SerializedName("y")
    private final double y;
}
