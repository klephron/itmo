package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;

@Introspected
@Serdeable
public record AddCaseRequest(@NotNull @NotEmpty String title, String description,
    @NotNull @Valid HashMap<@NotNull Long, @NotEmpty String> users) {
}
