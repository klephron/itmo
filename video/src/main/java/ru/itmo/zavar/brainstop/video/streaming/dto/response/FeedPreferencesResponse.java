package ru.itmo.zavar.brainstop.video.streaming.dto.response;

import java.util.List;

public record FeedPreferencesResponse(
        double likeWeight,
        double followWeight,
        double watchWeight,
        double watchThreshold,
        long recentEmbedded,
        long followingCount,
        long likedCount,
        long watchedCount,
        int preferenceVectors,
        int watchVectors,
        boolean preferenceNull,
        boolean watchPreferenceNull,
        List<Float> preferenceSample,
        List<Float> watchPreferenceSample,
        int topicVectors
) {
}
