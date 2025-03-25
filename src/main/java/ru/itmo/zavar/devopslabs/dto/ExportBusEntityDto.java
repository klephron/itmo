package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.ExportBusEntity}
 */
public record ExportBusEntityDto(Long id, @NotNull @NotEmpty String busName, @Min(0) int channelsUsed,
                                 @NotNull Long networkId) implements Serializable {
}