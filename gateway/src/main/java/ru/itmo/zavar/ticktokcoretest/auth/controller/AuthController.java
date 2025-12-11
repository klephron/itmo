package ru.itmo.zavar.ticktokcoretest.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.ticktokcoretest.auth.dto.request.LoginRequest;
import ru.itmo.zavar.ticktokcoretest.auth.dto.request.RegisterRequest;
import ru.itmo.zavar.ticktokcoretest.auth.dto.response.AuthResponse;
import ru.itmo.zavar.ticktokcoretest.auth.security.SecurityBeans;
import ru.itmo.zavar.ticktokcoretest.auth.service.AuthService;
import ru.itmo.zavar.ticktokcoretest.user.UserEntity;
import ru.itmo.zavar.ticktokcoretest.user.UserRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        UserEntity user = this.authService.register(req.username(), req.email(), req.displayName(), req.password());
        String token = this.authService.login(req.username(), req.password());
        String avatarUrl = user.getAvatarKey() == null ? null : "/api/users/" + user.getId() + "/avatar";
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoles(),
                avatarUrl
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        String token = this.authService.login(req.username(), req.password());
        UserEntity user = this.userRepository.findByUsername(req.username()).orElseThrow();
        String avatarUrl = user.getAvatarKey() == null ? null : "/api/users/" + user.getId() + "/avatar";
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoles(),
                avatarUrl
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal SecurityBeans.UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        String avatarUrl = userPrincipal.getAvatarUrl();
        return ResponseEntity.ok(new AuthResponse(
                null,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getDisplayName(),
                userPrincipal.getRoles(),
                avatarUrl
        ));
    }
}
