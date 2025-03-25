package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.ItemEntity}
 */
public record ItemDto(Long id, @NotNull @NotEmpty String itemName,
                      @NotNull @Min(0) Integer quantity) implements Serializable {
}