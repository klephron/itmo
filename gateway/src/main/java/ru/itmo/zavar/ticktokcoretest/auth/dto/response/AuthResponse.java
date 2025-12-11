package ru.itmo.zavar.ticktokcoretest.auth.dto.response;

import ru.itmo.zavar.ticktokcoretest.common.security.Role;

import java.util.Set;

public record AuthResponse(String token,
                           Long userId,
                           String username,
                           String displayName,
                           Set<Role> roles,
                           String avatarUrl) {
}
