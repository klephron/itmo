package ru.itmo.zavar.ticktokcoretest.common.api.video;

import java.util.UUID;

public record VideoMetadataSummary(
        Long id,
        UUID uuid,
        Long likesCount,
        Long viewsCount,
        Long uploaderId,
        String uploaderName,
        String contentType,
        Long contentSize,
        String thumbnailKey
) {
}
