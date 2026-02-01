package ru.itmo.zavar.brainstop.auth.dto.response;

import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.Set;

public record AuthResponse(String token,
                           Long userId,
                           String username,
                           String displayName,
                           Set<Role> roles,
                           String avatarUrl) {
}
