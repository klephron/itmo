package ru.itmo.zavar.brainstop.video.streaming.dto.request;

public record FeedPreferencesUpdateRequest(
        Double likeWeight,
        Double followWeight,
        Double watchWeight,
        Double watchThreshold
) {
}
