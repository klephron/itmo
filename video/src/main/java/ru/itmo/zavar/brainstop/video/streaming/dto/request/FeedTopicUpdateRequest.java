package ru.itmo.zavar.brainstop.video.streaming.dto.request;

public record FeedTopicUpdateRequest(
        String term,
        Double weight
) {
}
