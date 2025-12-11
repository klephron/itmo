package ru.itmo.zavar.ticktokcoretest.common.api.video.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserVideoDto(
        UUID uuid,
        String name,
        Long uploaderId,
        String uploaderName,
        Long contentSize,
        String contentType,
        Long likesCount,
        Long viewsCount,
        Instant uploadedAt,
        String thumbnailUrl,
        String uploaderAvatarUrl
) {
}
