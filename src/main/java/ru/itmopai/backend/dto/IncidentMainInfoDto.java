package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link ru.itmopai.backend.entity.IncidentEntity}
 */
@Introspected
@Serdeable
public record IncidentMainInfoDto(Long id, String applicant, @NotNull @NotEmpty String type,
    @NotNull @NotEmpty String reason, @NotNull Double locationLatitude,
    @NotNull Double locationLongitude, @NotNull Double locationAltitude,
    @NotNull LocalDateTime estimatedTime) implements Serializable {
}
