package ru.itmo.zavar.ticktokcoretest.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("roleEvaluator")
public class RoleEvaluator {

    public boolean hasRole(Authentication auth, Role role) {
        if (auth == null || !auth.isAuthenticated()) return false;

        String needed = role.authority(); // ROLE_...

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(needed));
    }
}


