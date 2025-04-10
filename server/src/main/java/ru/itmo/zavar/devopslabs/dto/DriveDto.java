package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.DriveEntity}
 */
public record DriveDto(Long id, @NotNull @NotEmpty String driveName, @NotNull @Min(0) Integer channelsUsed,
                       @NotNull Long networkId) implements Serializable {
}