package ru.itmo.zavar.ticktokcoretest.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.request.LoginRequest;
import ru.itmo.zavar.ticktokcoretest.dto.request.RegisterRequest;
import ru.itmo.zavar.ticktokcoretest.dto.response.AuthResponse;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans.UserPrincipal;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        UserEntity u = authService.register(
                req.username(),
                req.email(),
                req.displayName(),
                req.password()
        );
        String token = authService.login(req.username(), req.password());
        String avatarUrl = u.getAvatarKey() == null
                ? null
                : "/api/users/" + u.getId() + "/avatar";
        return ResponseEntity.ok(new AuthResponse(token, u.getId(), u.getUsername(), u.getDisplayName(), u.getRoles(), avatarUrl));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        String token = authService.login(req.username(), req.password());
        UserEntity u = userRepository.findByUsername(req.username())
                .orElseThrow();
        String avatarUrl = u.getAvatarKey() == null
                ? null
                : "/api/users/" + u.getId() + "/avatar";
        return ResponseEntity.ok(new AuthResponse(token, u.getId(), u.getUsername(), u.getDisplayName(), u.getRoles(), avatarUrl));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication, HttpServletRequest request) {
        if (authentication == null)
            return ResponseEntity.status(401).build();

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal up))
            return ResponseEntity.status(401).build();

        UserEntity u = userRepository.findById(up.getId())
                .orElseThrow();

        String avatarUrl = u.getAvatarKey() == null
                ? null
                : "/api/users/" + u.getId() + "/avatar";

        return ResponseEntity.ok(new AuthResponse(
                null, // token not returned
                u.getId(),
                u.getUsername(),
                u.getDisplayName(),
                u.getRoles(),
                avatarUrl
        ));
    }
}


