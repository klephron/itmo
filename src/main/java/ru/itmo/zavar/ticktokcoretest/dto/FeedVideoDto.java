package ru.itmo.zavar.ticktokcoretest.dto;

import java.time.Instant;
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

        boolean likedByMe,
        boolean subscribedToAuthor
) {
}
