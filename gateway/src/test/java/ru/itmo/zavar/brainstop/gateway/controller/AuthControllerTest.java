package ru.itmo.zavar.brainstop.gateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.auth.controller.AuthController;
import ru.itmo.zavar.brainstop.auth.dto.request.LoginRequest;
import ru.itmo.zavar.brainstop.auth.dto.request.RegisterRequest;
import ru.itmo.zavar.brainstop.auth.dto.response.AuthResponse;
import ru.itmo.zavar.brainstop.auth.service.AuthService;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.user.UserEntity;
import ru.itmo.zavar.brainstop.user.UserRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1", "FR-1.2"})
@Layer(TestLayer.BE_UNIT)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(Role.USER));
        testUser.setAvatarKey(null);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-AUTH-CTL-01: Успешная регистрация")
    void register_success() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "New User", "password123");

        when(authService.register(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUser);
        when(authService.login(anyString(), anyString())).thenReturn("jwt.token.here");

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("jwt.token.here");
        assertThat(response.getBody().username()).isEqualTo("testuser");
        assertThat(response.getBody().userId()).isEqualTo(1L);
        verify(authService).register("newuser", "new@example.com", "New User", "password123");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-AUTH-CTL-02: Успешный логин")
    void login_success() {
        LoginRequest request = new LoginRequest("testuser", "password123");

        when(authService.login(anyString(), anyString())).thenReturn("jwt.token.here");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("jwt.token.here");
        assertThat(response.getBody().username()).isEqualTo("testuser");
        verify(authService).login("testuser", "password123");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-AUTH-CTL-03: Получение данных текущего пользователя (JWT)")
    void me_withJwtUser() {
        CurrentUser currentUser = new CurrentUser(1L, "testuser", "Test User",
                "/api/users/1/avatar", Set.of(Role.USER));

        ResponseEntity<AuthResponse> response = authController.me(currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(1L);
        assertThat(response.getBody().username()).isEqualTo("testuser");
        assertThat(response.getBody().token()).isNull();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-AUTH-CTL-04: Запрос /me без авторизации")
    void me_noAuth() {
        ResponseEntity<AuthResponse> response = authController.me("invalid-principal");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-AUTH-CTL-05: Регистрация с аватаром")
    void register_withAvatar() {
        testUser.setAvatarKey("avatar.jpg");
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "New User", "password123");

        when(authService.register(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUser);
        when(authService.login(anyString(), anyString())).thenReturn("jwt.token");

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().avatarUrl()).isEqualTo("/api/users/1/avatar");
    }
}