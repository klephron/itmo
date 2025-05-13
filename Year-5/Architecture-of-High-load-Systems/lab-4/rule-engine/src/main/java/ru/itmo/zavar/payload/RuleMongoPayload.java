package ru.itmo.zavar.payload;

import jakarta.validation.constraints.*;

public record RuleMongoPayload(@NotNull @NotEmpty String ruleName, @NotNull @NotEmpty String replicaNumber, @NotNull @Positive long ruleFireTimestamp, @NotNull @Positive Long radiatorId, @NotNull @NotEmpty String payload) {

}
