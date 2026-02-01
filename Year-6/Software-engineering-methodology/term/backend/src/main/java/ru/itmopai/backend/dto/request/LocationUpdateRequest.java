package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;

@Introspected
@Serdeable
public record LocationUpdateRequest(@Nullable Double latitude, @Nullable Double longitude,
    @Nullable Double altitude) {
}
