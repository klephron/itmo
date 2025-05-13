package ru.itmo.zavar.account.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.account.entity.BillEntity}
 */
@Value
public class BillDto implements Serializable {
    @NotNull
    @NotEmpty
    String id;
    @NotNull
    @NotEmpty
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
