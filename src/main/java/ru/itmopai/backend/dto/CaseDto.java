package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseEntity}
 */
@Introspected
@Serdeable
public record CaseDto(Long id, @NotNull @NotEmpty String title, String description, Long creatorId,
                      @NotNull LocalDateTime createdAt, Long updatorId, @NotNull LocalDateTime updatedAt,
                      @NotNull CaseEntity.Status status, Long incidentId) implements Serializable {
}
