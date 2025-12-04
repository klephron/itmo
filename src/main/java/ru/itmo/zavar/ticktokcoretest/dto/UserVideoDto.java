package ru.itmo.zavar.ticktokcoretest.dto;

import java.time.Instant;
import java.util.UUID;

public record UserVideoDto(
        UUID uuid,
        String name,
        String uploaderName,
        Long contentSize,
        String contentType,
        Instant uploadedAt,
        String thumbnailUrl,
        String uploaderAvatarUrl
) {}
