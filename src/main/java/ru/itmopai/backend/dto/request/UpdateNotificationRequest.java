package ru.itmopai.backend.dto.request;

import java.time.LocalDateTime;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record UpdateNotificationRequest(String title, String description,
    LocalDateTime readAt) {
}
