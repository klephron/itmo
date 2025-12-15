package ru.itmo.zavar.ticktokcoretest.admin.dto;

import ru.itmo.zavar.ticktokcoretest.common.security.Role;

import java.util.Set;

public record UpdateUserRolesRequest(Set<Role> roles) {
}
