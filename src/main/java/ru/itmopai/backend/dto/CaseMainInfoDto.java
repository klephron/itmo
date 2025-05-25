package ru.itmopai.backend.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseEntity;

import java.io.Serializable;
import java.util.HashMap;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseEntity}
 */
@Introspected
@Serdeable
public record CaseMainInfoDto(Long id, @NotNull @NotEmpty String title, String description,
                              @NotNull CaseEntity.Status status) implements Serializable {
}
