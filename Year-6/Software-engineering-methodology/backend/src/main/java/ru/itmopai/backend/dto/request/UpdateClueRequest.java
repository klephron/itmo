package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import ru.itmopai.backend.entity.ClueEntity;

@Introspected
@Serdeable
public record UpdateClueRequest(@Nullable String title, @Nullable String description,
    @Nullable ClueEntity.Status status) {
}
