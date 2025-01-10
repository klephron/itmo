package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PostOfficePack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("address")
    private final AddressPack address;
    @SerializedName("coordinates")
    private final CoordinatesPack coordinates;

}
