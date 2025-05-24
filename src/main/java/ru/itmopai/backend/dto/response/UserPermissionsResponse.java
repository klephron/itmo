package ru.itmopai.backend.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Introspected
@Serdeable
public record UserPermissionsResponse(List<String> permissions) {
}
