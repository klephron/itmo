package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.StorageCellEntity}
 */
public record StorageCellEntityDto(Long id, @NotNull @Min(0) Integer capacity,
                                   @NotNull Long driveId) implements Serializable {
}