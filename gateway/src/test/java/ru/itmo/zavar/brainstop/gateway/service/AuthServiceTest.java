package ru.itmo.zavar.brainstop.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.itmo.zavar.brainstop.auth.security.SecurityBeans;
import ru.itmo.zavar.brainstop.auth.security.jwt.JwtTokenProvider;
import ru.itmo.zavar.brainstop.auth.service.AuthService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1", "FR-1.2"})
@Layer(TestLayer.BE_UNIT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setDisplayName("Test User");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(Role.USER));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-AUTH-01: Успешная регистрация")
    void register_success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        UserEntity result = authService.register("testuser", "test@example.com", "Test User", "password123");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRoles()).contains(Role.USER);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-AUTH-02: Регистрация с занятым username")
    void register_usernameTaken() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register("testuser", "test@example.com", "Test User", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-AUTH-03: Регистрация с занятым email")
    void register_emailTaken() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register("testuser", "test@example.com", "Test User", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already taken");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-AUTH-04: Успешный логин")
    void login_success() {
        Authentication auth = mock(Authentication.class);
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(any(UserEntity.class))).thenReturn("jwt.token.here");

        String token = authService.login("testuser", "password123");

        assertThat(token).isEqualTo("jwt.token.here");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(testUser);
    }
}
