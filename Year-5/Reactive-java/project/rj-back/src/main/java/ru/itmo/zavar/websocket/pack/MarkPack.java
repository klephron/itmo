package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MarkPack {
    @SerializedName("nominal")
    private final Integer nominal;
    @SerializedName("country")
    private final String country;
    @SerializedName("name")
    private final String name;
}
