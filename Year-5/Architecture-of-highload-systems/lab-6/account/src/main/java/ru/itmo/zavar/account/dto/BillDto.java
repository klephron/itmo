package ru.itmo.zavar.account.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

import java.io.Serializable;

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

    @JsonCreator
    public BillDto(
        @JsonProperty("id") String id,
        @JsonProperty("userId") String userId,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("heat") Long heat,
        @JsonProperty("cost") Long cost,
        @JsonProperty("payed") Boolean payed
    ) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
        this.heat = heat;
        this.cost = cost;
        this.payed = payed;
    }
}
