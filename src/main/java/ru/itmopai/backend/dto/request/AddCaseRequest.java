package ru.itmopai.backend.dto.request;

import java.util.HashMap;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Introspected
@Serdeable
public record AddCaseRequest(@NotNull @NotEmpty String title, @Nullable String description,
    @NotNull HashMap<@NotNull Long, @NotNull @NotEmpty String> users) {
}
