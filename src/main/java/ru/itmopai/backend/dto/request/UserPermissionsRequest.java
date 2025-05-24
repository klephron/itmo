package ru.itmopai.backend.dto.request;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record UserPermissionsRequest(List<String> permissions) {
}
