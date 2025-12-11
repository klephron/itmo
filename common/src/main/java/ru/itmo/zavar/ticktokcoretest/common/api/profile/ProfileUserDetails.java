package ru.itmo.zavar.ticktokcoretest.common.api.profile;

import ru.itmo.zavar.ticktokcoretest.common.security.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record ProfileUserDetails(
        Long id,
        String username,
        String displayName,
        String email,
        boolean enabled,
        Set<Role> roles,
        LocalDateTime createdAt,
        String avatarUrl
) {
}
