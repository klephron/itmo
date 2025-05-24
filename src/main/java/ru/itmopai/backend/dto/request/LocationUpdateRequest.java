package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

@Introspected
@Serdeable
public record LocationUpdateRequest(@NotNull Double latitude, @NotNull Double longitude, @NotNull Double altitude) {
}
