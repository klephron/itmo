package ru.itmo.zavar.brainstop.profile.dto;

public record PublicUserShortDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {}
