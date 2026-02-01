package ru.itmo.zavar.brainstop.common.api.profile.auth;

import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.Set;

public record AuthUser(
        Long id,
        String username,
        String email,
        String displayName,
        String passwordHash,
        Set<Role> roles,
        boolean enabled,
        String avatarKey
) {
}
