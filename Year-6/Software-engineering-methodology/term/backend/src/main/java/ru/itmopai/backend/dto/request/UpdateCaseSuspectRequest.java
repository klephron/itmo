package ru.itmopai.backend.dto.request;

import java.io.Serializable;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import ru.itmopai.backend.entity.CaseSuspectEntity;

@Introspected
@Serdeable
public record UpdateCaseSuspectRequest(@Nullable String name, @Nullable String surname,
    @Nullable String patronymic, @Nullable String identifier,
    @Nullable CaseSuspectEntity.Status status, @Nullable String verdict,
    @Nullable String accusation) implements Serializable {
}
