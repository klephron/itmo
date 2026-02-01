package ru.itmo.zavar.brainstop.common.api.video.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedVideoDto(
        UUID uuid,
        String name,
        Long uploaderId,
        String uploaderName,
        String uploaderAvatarUrl,

        Long contentSize,
        String contentType,
        Instant uploadedAt,

        Long likesCount,
        Long viewsCount,
        Long commentsCount,

        boolean likedByMe,
        boolean subscribedToAuthor,

        Integer defaultHeight,
        List<Integer> availableQualities,

        String description
) {
}
