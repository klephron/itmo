package ru.itmo.zavar.accountservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

import java.io.Serializable;

@Value
public class HeatCostDto implements Serializable {
    @NotNull
    @Positive
    Long timestamp;
    @NotNull
    @PositiveOrZero
    Long heat;
    @NotNull
    @PositiveOrZero
    Long cost;
}
