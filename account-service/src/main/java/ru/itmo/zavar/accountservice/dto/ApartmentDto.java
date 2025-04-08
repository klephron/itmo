package ru.itmo.zavar.accountservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.accountservice.entity.ApartmentEntity}
 */
@Value
public class ApartmentDto implements Serializable {
    @NotNull
    String id;
    String userId;
    @NotNull
    @NotEmpty
    String name;
}