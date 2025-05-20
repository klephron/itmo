package ru.itmo.zavar.payload;

import jakarta.validation.constraints.*;

public record RadiatorRabbitPayload(@NotNull @Min(1) Long deviceId, @Min(55) @Max(115) double radiatorTemp, @Min(4) @Max(43) double roomTemp, @NotNull long timestamp, @NotNull @PositiveOrZero Long heat) {

}
