package ru.itmopai.backend.dto.request;

import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;

@Introspected
@Serdeable
public record UpdateIncidentRequest(@Nullable String applicant, @Nullable String type,
    @Nullable String reason, @Nullable Double locationLatitude,
    @Nullable Double locationLongitude, @Nullable Double locationAltitude,
    @Nullable Instant estimatedTime) {
}
