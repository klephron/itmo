package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@link ru.itmopai.backend.entity.IncidentEntity}
 */
@Introspected
@Serdeable
public record IncidentDto(@NotNull Long id, @NotNull Long creatorId, @NotNull Instant createdAt,
    @NotNull Long updatorId,
    @NotNull Instant updatedAt, @Nullable String applicant, @NotNull @NotEmpty String type,
    @NotNull @NotEmpty String reason, @NotNull Double locationLatitude,
    @NotNull Double locationLongitude, @NotNull Double locationAltitude,
    @NotNull Instant estimatedTime, @Nullable Long relatedCaseId) implements Serializable {
}
