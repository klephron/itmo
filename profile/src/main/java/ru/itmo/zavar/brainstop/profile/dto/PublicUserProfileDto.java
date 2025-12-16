package ru.itmo.zavar.brainstop.profile.dto;

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
