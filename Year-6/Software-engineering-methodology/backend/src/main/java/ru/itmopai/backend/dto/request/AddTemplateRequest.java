package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;

@Introspected
@Serdeable
public record AddTemplateRequest(@NotEmpty String title, @NotEmpty String metadata,
    @Nullable String description) {
}
