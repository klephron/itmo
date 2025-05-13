package ru.itmo.zavar.accountservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

import java.io.Serializable;
import java.util.List;

@Value
public class HeatCostUserDto implements Serializable {
    @NotNull
    @NotEmpty
    String userId;
    @NotNull
    List<Long> timestamp;
    @NotNull
    List<Long> heat;
    @NotNull
    List<Long> cost;
}
