package ru.itmopai.backend.dto;

import java.io.Serializable;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.ClueEntity;

/**
 * DTO for {@link ru.itmopai.backend.entity.ClueEntity}
 */
@Introspected
@Serdeable
public record ClueMainInfoDto(@Nullable Long id, @NotNull @NotEmpty String title,
    @Nullable String description,
    @NotNull ClueEntity.Status status) implements Serializable {
}
