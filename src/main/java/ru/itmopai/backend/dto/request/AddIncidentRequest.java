package ru.itmopai.backend.dto.request;

import java.time.LocalDateTime;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Introspected
@Serdeable
public record AddIncidentRequest(String applicant, @NotNull @NotEmpty String type,
    @NotNull @NotEmpty String reason, @NotNull Double locationLatitude,
    @NotNull Double locationLongitude, @NotNull Double locationAltitude,
    @NotNull LocalDateTime estimatedTime) {
}
