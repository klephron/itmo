package ru.itmopai.backend.dto.request;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;

@Introspected
@Serdeable
public record UserPermissionsRequest(@Nullable List<String> permissions) {
}
