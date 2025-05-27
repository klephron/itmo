package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.TemplateEntity;

@Introspected
@Serdeable
public record UpdateTemplateRequest(@NotEmpty String title,
    @NotNull TemplateEntity.Status status, @NotEmpty String metadata,
    String description) {
}
