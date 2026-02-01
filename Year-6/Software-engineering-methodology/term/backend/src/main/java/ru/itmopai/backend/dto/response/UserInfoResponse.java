package ru.itmopai.backend.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record UserInfoResponse(Long id, String username) {
}
