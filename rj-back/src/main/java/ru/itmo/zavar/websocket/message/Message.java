package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Message {
    @SerializedName("op")
    private final String op;

}
