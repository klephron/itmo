package ru.itmopai.backend.dto.request;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseEntity;

import java.util.HashMap;

@Introspected
@Serdeable
public record UpdateCaseRequest(@NotEmpty String title, String description,
    CaseEntity.Status status,
    @Valid HashMap<@NotNull Long, @NotEmpty String> users) {
}
