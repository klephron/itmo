package ru.itmopai.backend.dto.request;

import java.util.HashMap;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseEntity;

@Introspected
@Serdeable
public record UpdateCaseRequest(@Nullable String title, @Nullable String description,
    @Nullable CaseEntity.Status status,
    @Nullable @Valid HashMap<@NotNull Long, @NotEmpty String> users) {
}
