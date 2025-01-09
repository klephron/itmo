package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class SubscribeMessage extends Message {
    public static final String OP = "subscribe";

    @SerializedName("args")
    private final Args args;

    public SubscribeMessage(Integer period) {
        super(OP);
        this.args = new Args(period);
    }

    @RequiredArgsConstructor
    @Getter
    public class Args {
        @SerializedName("period")
        private final Integer period;
    }
}
