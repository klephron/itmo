package ru.itmo.zavar.ticktokcoretest.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.model.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.Role;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans.UserPrincipal;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;

import java.util.Set;

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
        return ResponseEntity.ok(new AuthResponse(token, u.getId(), u.getUsername(), u.getDisplayName(), u.getRoles()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        String token = authService.login(req.username(), req.password());
        UserEntity u = userRepository.findByUsername(req.username())
                .orElseThrow();
        return ResponseEntity.ok(new AuthResponse(token, u.getId(), u.getUsername(), u.getDisplayName(), u.getRoles()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication, HttpServletRequest request) {
        System.out.println("Authorization header = " + request.getHeader("Authorization"));
        System.out.println("Authentication in method = " + authentication);

        if (authentication == null)
            return ResponseEntity.status(401).build();

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal up))
            return ResponseEntity.status(401).build();

        UserEntity u = userRepository.findById(up.getId())
                .orElseThrow();

        return ResponseEntity.ok(new AuthResponse(
                null, // token not returned
                u.getId(),
                u.getUsername(),
                u.getDisplayName(),
                u.getRoles()
        ));
    }

    public record RegisterRequest(String username, String email, String displayName, String password) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResponse(String token, Long userId, String username, String displayName, Set<Role> roles) {}
}


