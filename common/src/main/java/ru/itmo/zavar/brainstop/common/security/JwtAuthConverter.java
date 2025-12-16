package ru.itmo.zavar.brainstop.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<Role> roles = extractRoles(jwt);
        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(Role::authority)
                .map(SimpleGrantedAuthority::new)
                .toList();

        CurrentUser principal = new CurrentUser(
                extractUserId(jwt),
                jwt.getSubject(),
                jwt.getClaimAsString("displayName"),
                jwt.getClaimAsString("avatarKey"),
                roles
        );

        return new UsernamePasswordAuthenticationToken(principal, jwt.getTokenValue(), authorities);
    }

    private Long extractUserId(Jwt jwt) {
        Object raw = jwt.getClaim("userId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return raw == null ? null : Long.valueOf(raw.toString());
    }

    private Set<Role> extractRoles(Jwt jwt) {
        Object raw = jwt.getClaims().get("roles");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::toUpperCase)
                    .map(Role::valueOf)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        return Set.of();
    }
}
