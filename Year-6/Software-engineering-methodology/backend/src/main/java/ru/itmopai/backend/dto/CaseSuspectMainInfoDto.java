package ru.itmopai.backend.dto;

import java.io.Serializable;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseSuspectEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseSuspectEntity}
 */
@Introspected
@Serdeable
public record CaseSuspectMainInfoDto(@NotNull Long id, String name,
    String surname, String patronymic,
    String identifier,
    CaseSuspectEntity.Status status, String verdict, String accusation) implements Serializable {
}
