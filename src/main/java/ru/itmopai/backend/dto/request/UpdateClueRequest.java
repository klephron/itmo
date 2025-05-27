package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import ru.itmopai.backend.entity.ClueEntity;

@Introspected
@Serdeable
public record UpdateClueRequest(String title, String description,
    ClueEntity.Status status) {
}
