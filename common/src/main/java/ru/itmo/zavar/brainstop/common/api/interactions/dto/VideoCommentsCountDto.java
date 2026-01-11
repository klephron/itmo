package ru.itmo.zavar.brainstop.common.api.interactions.dto;

import java.util.UUID;

public record VideoCommentsCountDto(UUID videoUuid, long commentsCount) {
}
