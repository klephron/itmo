package ru.itmopai.backend.dto.request;

import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record UpdateIncidentRequest(String applicant, String type,
    String reason, Double locationLatitude,
    Double locationLongitude, Double locationAltitude,
    Instant estimatedTime) {
}
