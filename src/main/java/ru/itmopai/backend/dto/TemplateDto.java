package ru.itmopai.backend.dto;

import java.io.Serializable;
import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.TemplateEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.TemplateEntity}
 */
@Introspected
@Serdeable
public record TemplateDto(@Nullable Long id, @Nullable Long creatorId, @NotNull Instant createdAt,
    @Nullable Long updatorId,
    @NotNull Instant updatedAt, @NotNull @NotEmpty String title,
    @NotNull TemplateEntity.Status status, @NotNull @NotEmpty String metadata,
    @Nullable String description,
    @Nullable String uri, @Nullable String cdbURI,
    @Nullable Long relatedCaseId) implements Serializable {
}
