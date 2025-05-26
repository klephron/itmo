package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;

@Introspected
@Serdeable
public record UpdateTemplateRequest(@NotEmpty String title,
    @NotEmpty String status, @NotEmpty String metadata,
    String description) {
}
