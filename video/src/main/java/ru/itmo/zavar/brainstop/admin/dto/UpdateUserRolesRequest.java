package ru.itmo.zavar.brainstop.admin.dto;

import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.Set;

public record UpdateUserRolesRequest(
        Set<Role> roles
) {}
