package ru.itmo.zavar.payload;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record RadiatorRabbitPayload(@NotNull @Min(1) Long deviceId, @Min(55) @Max(115) double radiatorTemp, @Min(4) @Max(43) double roomTemp, @NotNull long timestamp) {

}
