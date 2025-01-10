package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MailPack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("subject")
    private final String subject;
    @SerializedName("sender_id")
    private final String senderId;
    @SerializedName("receiver_id")
    private final String receiverId;
    @SerializedName("text")
    private final String text;
    @SerializedName("value")
    private final Long value;
    @SerializedName("marks")
    private final List<MarkPack> marks;
    @SerializedName("status")
    private final String status;
    @SerializedName("created_at")
    private final String createdAt;
    @SerializedName("applied_at")
    private final String appliedAt;
    @SerializedName("sent_at")
    private final String sentAt;
    @SerializedName("received_at")
    private final String receivedAt;
}
