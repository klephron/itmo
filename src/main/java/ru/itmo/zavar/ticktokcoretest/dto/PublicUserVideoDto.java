package ru.itmo.zavar.ticktokcoretest.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicUserVideoDto(
        UUID uuid,
        String name,
        String thumbnailUrl,
        Instant uploadedAt
) {}
