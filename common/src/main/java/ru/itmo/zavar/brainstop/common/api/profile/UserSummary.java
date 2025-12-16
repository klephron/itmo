package ru.itmo.zavar.brainstop.common.api.profile;

public record UserSummary(
        Long id,
        String displayName,
        String avatarUrl
) {
}
