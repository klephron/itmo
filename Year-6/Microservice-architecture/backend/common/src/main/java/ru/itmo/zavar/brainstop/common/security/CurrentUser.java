package ru.itmo.zavar.brainstop.common.security;

import lombok.Getter;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight principal reconstructed from JWT claims.
 */
public class CurrentUser implements Principal {

    @Getter
    private final Long id;
    @Getter
    private final String username;
    @Getter
    private final String displayName;
    private final String avatarKey;
    @Getter
    private final Set<Role> roles;

    public CurrentUser(Long id,
                       String username,
                       String displayName,
                       String avatarKey,
                       Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarKey = avatarKey;
        this.roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public String getAvatarUrl() {
        if (avatarKey == null) {
            return null;
        }
        return "/api/users/" + id + "/avatar";
    }

    @Override
    public String getName() {
        return username;
    }
}
