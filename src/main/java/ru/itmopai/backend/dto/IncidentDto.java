package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link ru.itmopai.backend.entity.IncidentEntity}
 */
@Introspected
@Serdeable
public record IncidentDto(Long id, Long creatorId, @NotNull LocalDateTime createdAt, Long updatorId,
                          @NotNull LocalDateTime updatedAt, String applicant, @NotNull @NotEmpty String type,
                          @NotNull @NotEmpty String reason, @NotNull Double locationLatitude,
                          @NotNull Double locationLongitude, @NotNull Double locationAltitude,
                          @NotNull LocalDateTime estimatedTime, Long relatedCaseId, String relatedCaseTitle,
                          String relatedCaseDescription, Long relatedCaseCreatorId, LocalDateTime relatedCaseCreatedAt,
                          Long relatedCaseUpdatorId, LocalDateTime relatedCaseUpdatedAt,
                          CaseEntity.Status relatedCaseStatus, Long relatedCaseIncidentId) implements Serializable {
}
