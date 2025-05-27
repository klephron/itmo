package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import ru.itmopai.backend.entity.TemplateEntity;

@Introspected
@Serdeable
public record UpdateTemplateRequest(@Nullable String title,
    @Nullable TemplateEntity.Status status, @Nullable String metadata,
    @Nullable String description) {
}
