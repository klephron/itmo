package ru.itmo.zavar.accountservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link ru.itmo.zavar.accountservice.entity.PayHistoryEntity}
 */
@Value
public class PayHistoryDto implements Serializable {
    @NotNull
    @NotEmpty
    String id;
    @NotNull
    @NotEmpty
    String billId;
    @NotNull
    @Positive
    Long timestamp;
}