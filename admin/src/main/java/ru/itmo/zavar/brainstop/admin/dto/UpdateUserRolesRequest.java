package ru.itmo.zavar.brainstop.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty
        Set<Role> roles
) {
}
