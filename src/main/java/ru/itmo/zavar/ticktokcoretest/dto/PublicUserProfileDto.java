package ru.itmo.zavar.ticktokcoretest.dto;

import lombok.Builder;

@Builder
public record PublicUserProfileDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        long followersCount,
        long followingCount,
        boolean following
) {
}
