package ru.itmo.zavar.ticktokcoretest.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record UserVideoDto(
        UUID uuid,
        String name,
        String uploaderName,
        Long contentSize,
        String contentType,
        Instant uploadedAt,
        String thumbnailUrl,
        String uploaderAvatarUrl,
        long likesCount
) {}
