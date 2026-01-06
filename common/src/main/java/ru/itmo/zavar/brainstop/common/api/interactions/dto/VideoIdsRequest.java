package ru.itmo.zavar.brainstop.common.api.interactions.dto;

import java.util.Collection;

public record VideoIdsRequest(Long userId, Collection<Long> videoIds) {
}
