package ru.itmopai.backend.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public record LoginResponse(String accessToken,
    String refreshToken,
    Integer expiresIn,
    String username,
    Long id,
    Long iat) {
}
