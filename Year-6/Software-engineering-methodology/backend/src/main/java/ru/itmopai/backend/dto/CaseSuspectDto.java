package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import ru.itmopai.backend.entity.CaseSuspectEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseSuspectEntity}
 */
@Introspected
@Serdeable
public record CaseSuspectDto(Long id, Long creatorId, Instant createdAt,
    Long updatorId,
    Instant updatedAt, String name, String surname,
    String patronymic, String identifier,
    CaseSuspectEntity.Status status,
    Long relatedCaseId, String verdict, String accusation) implements Serializable {
}
