package ru.itmo.zavar.ticktokcoretest.dto;

public record PublicUserShortDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {}
