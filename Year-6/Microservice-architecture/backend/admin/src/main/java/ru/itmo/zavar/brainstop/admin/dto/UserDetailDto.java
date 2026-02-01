package ru.itmo.zavar.brainstop.admin.dto;

import ru.itmo.zavar.brainstop.common.security.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDetailDto(
        Long id,
        String username,
        String displayName,
        String email,
        boolean enabled,
        Set<Role> roles,
        LocalDateTime createdAt
) {
}
