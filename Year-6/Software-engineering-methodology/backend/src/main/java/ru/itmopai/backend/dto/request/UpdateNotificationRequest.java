package ru.itmopai.backend.dto.request;

import java.time.Instant;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record UpdateNotificationRequest(@Nullable String title, @Nullable String description,
    @Nullable Instant readAt) {
}
