package ru.itmo.zavar.brainstop.common.api.profile;

public record UserSummaryDto(
        Long id,
        String displayName,
        String avatarUrl
) {
}
