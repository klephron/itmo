package ru.itmo.zavar.brainstop.common.api.interactions.dto;

import java.util.Collection;
import java.util.UUID;

public record VideoUuidsRequest(Collection<UUID> videoUuids) {
}
