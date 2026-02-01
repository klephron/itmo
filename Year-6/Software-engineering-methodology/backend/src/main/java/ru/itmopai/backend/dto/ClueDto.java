package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.ClueEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.ClueEntity}
 */
@Introspected
@Serdeable
public record ClueDto(@NotNull Long id, @NotNull Long creatorId, @NotNull Instant createdAt,
    @NotNull Long updatorId, @NotNull Instant updatedAt, @NotNull @NotEmpty String title,
    @Nullable String description,
    @NotNull ClueEntity.Status status, @Nullable String metadata,
    @NotNull Long relatedCaseId) implements Serializable {
}
