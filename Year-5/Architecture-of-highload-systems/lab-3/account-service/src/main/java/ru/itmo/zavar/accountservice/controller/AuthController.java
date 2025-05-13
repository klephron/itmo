package ru.itmo.zavar.accountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.accountservice.dto.request.AuthRequest;
import ru.itmo.zavar.accountservice.dto.request.RegisterRequest;
import ru.itmo.zavar.accountservice.dto.response.AuthResponse;
import ru.itmo.zavar.accountservice.security.Roles;
import ru.itmo.zavar.accountservice.security.jwt.JwtUtil;
import ru.itmo.zavar.accountservice.service.UserService;

import java.util.ArrayList;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Регистрация и вход пользователей")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная регистрация"),
            @ApiResponse(responseCode = "400", description = "Ошибка регистрации", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для регистрации",
            required = true,
            content = @Content(schema = @Schema(implementation = RegisterRequest.class))
    ) @RequestBody RegisterRequest req) {
        try {
            ArrayList<String> authorities = new ArrayList<>();
            authorities.add(Roles.USER.toString());
            this.userService.register(req.getUsername(), req.getPassword(), req.getEmail(), authorities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok("Registered");
    }

    @Operation(summary = "Вход пользователя и получение JWT токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для входа",
            required = true,
            content = @Content(schema = @Schema(implementation = AuthRequest.class))
    ) @RequestBody AuthRequest req) {
        try {
            if (!this.userService.checkPassword(req.getUsername(), req.getPassword())) {
                return ResponseEntity.status(401).body("Invalid credentials");
            }
            UserDetails user = this.userService.loadUserByUsername(req.getUsername());
            String token = this.jwtUtil.generateToken(user);
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
