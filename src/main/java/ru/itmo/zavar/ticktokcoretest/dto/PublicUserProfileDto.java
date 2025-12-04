package ru.itmo.zavar.ticktokcoretest.dto;

public record PublicUserProfileDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {}