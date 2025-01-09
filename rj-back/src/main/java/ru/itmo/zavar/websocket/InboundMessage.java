package ru.itmo.zavar.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InboundMessage {
    @JsonProperty("op")
    private final String op;

    public InboundMessage(String op) {
        this.op = op;
    }
}
