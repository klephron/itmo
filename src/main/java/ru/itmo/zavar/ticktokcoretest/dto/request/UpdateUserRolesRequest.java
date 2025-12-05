package ru.itmo.zavar.ticktokcoretest.dto.request;

import ru.itmo.zavar.ticktokcoretest.security.Role;

import java.util.Set;

public record UpdateUserRolesRequest(
        Set<Role> roles
) {}
