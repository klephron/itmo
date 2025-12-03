package ru.itmo.zavar.ticktokcoretest.service;

import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.model.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.JwtTokenProvider;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public UserEntity register(String username,
                               String email,
                               String displayName,
                               String rawPassword) {

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already taken");
        }

        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setEnabled(true);

        return userRepository.save(u);
    }

    public String login(String username, String rawPassword) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword)
        );
        var principal = (SecurityBeans.UserPrincipal) auth.getPrincipal();
        UserEntity user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow();
        return jwtTokenProvider.generateToken(user.getId(), user.getUsername());
    }
}

