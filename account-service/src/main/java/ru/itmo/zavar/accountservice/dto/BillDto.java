package ru.itmo.zavar.accountservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.accountservice.entity.BillEntity}
 */
@Value
public class BillDto implements Serializable {
    @NotNull
    String id;
    @NotNull
    String userId;
    @NotNull
    @Positive
    Long timestamp;
    @NotNull
    @PositiveOrZero
    Long heat;
    @NotNull
    @PositiveOrZero
    Long cost;
    @NotNull
    Boolean payed;
}