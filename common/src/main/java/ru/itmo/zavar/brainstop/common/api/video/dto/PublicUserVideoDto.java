package ru.itmo.zavar.brainstop.common.api.video.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicUserVideoDto(
        UUID uuid,
        String name,
        String thumbnailUrl,
        Instant uploadedAt,
        long likesCount,
        long viewsCount,
        Long contentSize,
        Integer defaultHeight,
        List<Integer> availableQualities,
        String description,
        Long uploaderId,
        String uploaderName,
        String uploaderAvatarUrl
) {
}
