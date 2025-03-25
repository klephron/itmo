package ru.itmo.zavar.devopslabs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.devopslabs.entity.NetworkEntity}
 */
public record NetworkDto(Long id, @NotNull @NotEmpty String name, @NotNull @Min(0) Integer channelCapacity, @NotNull @Min(0) Integer usedChannels) implements Serializable {
  }