package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.Role;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;
import ru.itmo.zavar.ticktokcoretest.security.jwt.JwtTokenProvider;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public UserEntity register(String username, String email, String displayName, String rawPassword) {
        if (this.userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (this.userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already taken");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setEmail(email);
        userEntity.setDisplayName(displayName);
        userEntity.setPassword(this.passwordEncoder.encode(rawPassword));
        userEntity.setEnabled(true);
        userEntity.getRoles().add(Role.USER);

        return this.userRepository.save(userEntity);
    }

    public String login(String username, String rawPassword) {
        Authentication auth = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, rawPassword));
        var principal = (SecurityBeans.UserPrincipal) auth.getPrincipal();
        UserEntity user = this.userRepository.findByUsername(principal.getUsername())
                .orElseThrow();
        return this.jwtTokenProvider.generateToken(user.getId(), user.getUsername());
    }

    public static Long extractUserId(Authentication authentication) {
        if (authentication == null)
            throw new RuntimeException("Unauthenticated");
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> map && map.get("id") != null) {
            return Long.valueOf(map.get("id").toString());
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getId();
        }
        throw new RuntimeException("Cannot extract user id");
    }

    public static String extractDisplayName(Authentication auth) {
        if (auth == null) return "Аноним";
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getDisplayName();
        }
        return "Аноним";
    }
}

