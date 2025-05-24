package ru.itmopai.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.itmopai.backend.entity.CaseEntity;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmopai.backend.entity.CaseEntity}
 */
public record CaseMainInfoDto(Long id, @NotNull @NotEmpty String title, String description,
                              @NotNull CaseEntity.Status status) implements Serializable {
}
