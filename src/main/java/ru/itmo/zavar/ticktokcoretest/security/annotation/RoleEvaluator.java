package ru.itmo.zavar.ticktokcoretest.security.annotation;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.security.Role;

@Component("roleEvaluator")
public class RoleEvaluator {

    public boolean hasRole(Authentication auth, Role role) {
        if (auth == null || !auth.isAuthenticated()) return false;

        String needed = role.authority();

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(needed));
    }
}


