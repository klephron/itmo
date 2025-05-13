package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.ImportBusEntity}
 */
public record ImportBusDto(Long id, @NotNull @NotEmpty String busName, @NotNull @Min(0) Integer channelsUsed,
                           @NotNull Long networkId) implements Serializable {
}