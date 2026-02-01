package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Introspected
@Serdeable
public record UserDto(@NotNull Long id, @NotNull @NotEmpty String username,
    @NotNull @NotEmpty String name,
    @NotNull @NotEmpty String surname, @Nullable String patronymic,
    @Nullable String primaryPosition, @NotNull Double locationLatitude,
    @NotNull Double locationLongitude, @NotNull Double locationAltitude) {
}
