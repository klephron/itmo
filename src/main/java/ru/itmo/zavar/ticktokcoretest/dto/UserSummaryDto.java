package ru.itmo.zavar.ticktokcoretest.dto;

import ru.itmo.zavar.ticktokcoretest.security.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record UserSummaryDto(
        Long id,
        String username,
        String displayName,
        String email,
        boolean enabled,
        Set<Role> roles,
        LocalDateTime createdAt
) {}
