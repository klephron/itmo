package ru.itmo.zavar.brainstop.common.api.interactions.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.UUID;

public record VideoUuidsRequest(
        @NotNull
        Collection<UUID> videoUuids
) {
}
