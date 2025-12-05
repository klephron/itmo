package ru.itmo.zavar.ticktokcoretest.dto.response;

import ru.itmo.zavar.ticktokcoretest.security.Role;

import java.util.Set;

public record AuthResponse(String token, Long userId, String username, String displayName, Set<Role> roles, String avatarUrl) {}
