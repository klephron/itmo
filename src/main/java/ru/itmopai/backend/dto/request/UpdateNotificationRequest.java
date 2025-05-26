package ru.itmopai.backend.dto.request;

import java.time.LocalDateTime;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;

@Introspected
@Serdeable
public record UpdateNotificationRequest(@NotEmpty String title, String description,
    LocalDateTime readAt) {
}
