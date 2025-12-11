package ru.itmo.zavar.ticktokcoretest.common.api.profile;

public record UserSummary(
        Long id,
        String displayName,
        String avatarUrl
) {
}
