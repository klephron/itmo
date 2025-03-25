package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.PatternEntity}
 */
public record PatternEntityDto(Long id, @NotNull @NotEmpty String patternName, String inputItems,
                               @NotNull @NotEmpty String outputItemName, @NotNull @Min(0) Integer outputQuantity,
                               @NotNull Long meInterfaceId) implements Serializable {
}