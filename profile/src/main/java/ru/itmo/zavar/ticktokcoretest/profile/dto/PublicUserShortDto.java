package ru.itmo.zavar.ticktokcoretest.profile.dto;

public record PublicUserShortDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {}
