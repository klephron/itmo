package ru.itmo.zavar.ticktokcoretest.dto;

import java.time.Instant;

public record CommentDto(
        Long id,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String text,
        Instant createdAt,
        boolean mine
) {}

