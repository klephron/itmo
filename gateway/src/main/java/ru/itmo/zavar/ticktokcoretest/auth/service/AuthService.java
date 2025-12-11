package ru.itmo.zavar.ticktokcoretest.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;
import ru.itmo.zavar.ticktokcoretest.auth.security.SecurityBeans;
import ru.itmo.zavar.ticktokcoretest.auth.security.jwt.JwtTokenProvider;
import ru.itmo.zavar.ticktokcoretest.user.UserEntity;
import ru.itmo.zavar.ticktokcoretest.user.UserRepository;

import java.util.Set;

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

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPassword(this.passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setRoles(Set.of(Role.USER));

        return this.userRepository.save(user);
    }

    public String login(String username, String rawPassword) {
        Authentication auth = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword)
        );
        SecurityBeans.UserPrincipal principal = (SecurityBeans.UserPrincipal) auth.getPrincipal();
        UserEntity user = this.userRepository.findById(principal.getId()).orElseThrow();
        return this.jwtTokenProvider.generateToken(user);
    }
}
