package ru.itmo.zavar.brainstop.common.api.interactions.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Collection;

public record VideoIdsRequest(
        @NotNull
        Long userId,
        @NotNull
        Collection<Long> videoIds
) {
}
