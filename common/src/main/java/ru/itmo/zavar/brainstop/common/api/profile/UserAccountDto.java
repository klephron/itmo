package ru.itmo.zavar.brainstop.common.api.profile;

import ru.itmo.zavar.brainstop.common.security.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record UserAccountDto(
        Long id,
        String username,
        String displayName,
        String email,
        boolean enabled,
        Set<Role> roles,
        LocalDateTime createdAt,
        String avatarKey
) {
}
