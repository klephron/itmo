package ru.itmo.zavar.payload;

import jakarta.validation.constraints.*;

public record RadiatorMqttPayload(@NotNull @Min(1) Long deviceId, @Min(55) @Max(115) double radiatorTemp, @Min(4) @Max(43) double roomTemp, @NotNull Long timestamp, @NotNull @PositiveOrZero Long heat) {

}
