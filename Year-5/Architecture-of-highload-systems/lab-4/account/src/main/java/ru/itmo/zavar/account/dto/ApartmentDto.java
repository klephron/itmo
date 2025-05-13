package ru.itmo.zavar.account.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.account.entity.ApartmentEntity}
 */
@Value
public class ApartmentDto implements Serializable {
    @NotNull
    @NotEmpty
    String id;
    @NotNull
    @NotEmpty
    String userId;
    @NotNull
    @NotEmpty
    String name;
}
