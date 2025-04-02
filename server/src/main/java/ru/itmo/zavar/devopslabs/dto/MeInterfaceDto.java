package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity}
 */
public record MeInterfaceDto(Long id, @NotNull @NotEmpty String interfaceName, @Min(0) int channelsUsed,
                             @NotNull Long networkId) implements Serializable {
}