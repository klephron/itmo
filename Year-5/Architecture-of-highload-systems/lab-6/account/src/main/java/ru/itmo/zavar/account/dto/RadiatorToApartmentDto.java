package ru.itmo.zavar.account.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.account.entity.RadiatorToApartmentEntity}
 */
@Value
public class RadiatorToApartmentDto implements Serializable {
    @NotNull
    @NotEmpty
    String apartmentId;
    @NotNull
    Long deviceId;
}
