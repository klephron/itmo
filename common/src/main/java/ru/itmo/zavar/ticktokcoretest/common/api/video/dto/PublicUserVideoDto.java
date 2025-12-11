package ru.itmo.zavar.ticktokcoretest.common.api.video.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicUserVideoDto(
        UUID uuid,
        String name,
        String thumbnailUrl,
        Instant uploadedAt,
        long likesCount,
        long viewsCount
) {
}
