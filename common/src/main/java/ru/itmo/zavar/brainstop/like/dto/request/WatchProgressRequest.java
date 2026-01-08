package ru.itmo.zavar.brainstop.like.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record WatchProgressRequest(
        @PositiveOrZero
        Long positionSeconds,
        @PositiveOrZero
        Long durationSeconds,
        @Min(0)
        @Max(100)
        Integer percent
) {
}
