package ru.itmo.zavar.ticktokcoretest.auth.security.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;
import ru.itmo.zavar.ticktokcoretest.auth.security.SecurityBeans;
import ru.itmo.zavar.ticktokcoretest.user.UserEntity;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Claims claims = jwtTokenProvider.getClaims(token);
                    SecurityBeans.UserPrincipal principal = buildPrincipal(claims);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );

                    Map<String, Object> details = new java.util.HashMap<>();
                    details.put("id", principal.getId());
                    auth.setDetails(details);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private SecurityBeans.UserPrincipal buildPrincipal(Claims claims) {
        Long userId = claims.get("userId", Number.class).longValue();
        String username = claims.getSubject();
        String displayName = claims.get("displayName", String.class);
        String avatarKey = claims.get("avatarKey", String.class);
        List<String> rolesRaw = claims.get("roles", List.class);

        Set<Role> roles = new HashSet<>();
        if (rolesRaw != null) {
            rolesRaw.stream()
                    .map(String::toUpperCase)
                    .map(Role::valueOf)
                    .forEach(roles::add);
        }

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setAvatarKey(avatarKey);
        user.setRoles(roles);
        user.setEnabled(true);

        return new SecurityBeans.UserPrincipal(user);
    }
}
