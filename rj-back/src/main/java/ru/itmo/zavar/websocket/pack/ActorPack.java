package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
    @SerializedName("mails_buffer_capacity")
    private final int bufferCapacity;
    @SerializedName("mails_buffer")
    private final int bufferCurrentSize;
    @SerializedName("mails_count")
    private final long mailsCount;
    @SerializedName("buffer")
    private final List<MailPack> buffer;

    public enum Kind {
        @SerializedName("INDIVIDUAL")
        INDIVIDUAL,
        @SerializedName("ORGANIZATION")
        ORGANIZATION,
    }
}
