package ru.itmo.zavar.brainstop.interactions.dto;

import ru.itmo.zavar.brainstop.interactions.model.ComplaintStatus;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintTargetType;

import java.time.Instant;
import java.util.UUID;

public record ComplaintDto(
        UUID id,
        ComplaintTargetType targetType,
        UUID videoUuid,
        Long commentId,
        String reason,
        Long authorId,
        ComplaintStatus status,
        Long moderatorId,
        String resolution,
        Instant createdAt
) {
}
