package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Introspected
@Serdeable
public record RegisterRequest(@NotNull @NotEmpty String username,
    @NotNull @NotEmpty String password, @NotNull @NotEmpty String name,
    @NotNull @NotEmpty String surname,
    @Nullable String patronymic, @Nullable String primaryPosition) {
}
