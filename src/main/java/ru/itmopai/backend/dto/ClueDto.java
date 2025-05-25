package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.ClueEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.ClueEntity}
 */
@Introspected
@Serdeable
public record ClueDto(Long id, Long creatorId, @NotNull LocalDateTime createdAt, Long updatorId,
    @NotNull LocalDateTime updatedAt, @NotNull @NotEmpty String title, String description,
    @NotNull ClueEntity.Status status, String metadata,
    Long relatedCaseId) implements Serializable {
}
