package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record LocationUpdateRequest(Double latitude, Double longitude, Double altitude) {
}
