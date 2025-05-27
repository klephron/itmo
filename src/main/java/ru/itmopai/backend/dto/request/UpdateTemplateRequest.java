package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import ru.itmopai.backend.entity.TemplateEntity;

@Introspected
@Serdeable
public record UpdateTemplateRequest(String title,
    TemplateEntity.Status status, String metadata,
    String description) {
}
