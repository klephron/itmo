package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MailPack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("subject")
    private final String subject;
    @SerializedName("sender_actor_id")
    private final Long senderActorId;
    @SerializedName("receiver_actor_id")
    private final Long receiverActorId;
    @SerializedName("text")
    private final String text;
    @SerializedName("value")
    private final Long value;
    @SerializedName("marks")
    private final List<MarkPack> marks;
    @SerializedName("status")
    private final Status status;
    @SerializedName("created_at")
    private final String createdAt;
    @SerializedName("applied_at")
    private final String appliedAt;
    @SerializedName("sent_at")
    private final String sentAt;
    @SerializedName("received_at")
    private final String receivedAt;

    public enum Status {
        @SerializedName("CREATED")
        CREATED,
        @SerializedName("APPLIED")
        APPLIED,
        @SerializedName("SENT")
        SENT,
        @SerializedName("RECEIVED")
        RECEIVED,
    }
}
