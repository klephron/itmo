package ru.itmo.zavar.accountservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.accountservice.dto.request.AuthRequest;
import ru.itmo.zavar.accountservice.dto.request.RegisterRequest;
import ru.itmo.zavar.accountservice.dto.response.AuthResponse;
import ru.itmo.zavar.accountservice.security.jwt.JwtUtil;
import ru.itmo.zavar.accountservice.service.UserService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            userService.register(req.getUsername(), req.getPassword(), req.getAuthorities());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        if (!userService.checkPassword(req.getUsername(), req.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        UserDetails user = userService.loadUserByUsername(req.getUsername());
        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }
}