package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@link ru.itmopai.backend.entity.NotificationEntity}
 */
@Introspected
@Serdeable
public record NotificationDto(Long id, Long userId, @NotNull @NotEmpty String title,
    String description,
    @NotNull Instant createdAt, @NotNull Instant readAt) implements Serializable {
}
