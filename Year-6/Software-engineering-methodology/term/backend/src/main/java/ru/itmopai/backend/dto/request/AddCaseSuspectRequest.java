package ru.itmopai.backend.dto.request;

import java.io.Serializable;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseSuspectEntity;

@Introspected
@Serdeable
public record AddCaseSuspectRequest(@NotNull String name, @NotNull String surname,
    @Nullable String patronymic, @NotNull String identifier,
    @NotNull CaseSuspectEntity.Status status, @Nullable String verdict,
    @Nullable String accusation) implements Serializable {
}
