package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotEmpty;

@Introspected
@Serdeable
public record AddUpdateClueRequest(@NotEmpty String title, @NotEmpty String description,
    @NotEmpty String status) {
}
