package ru.itmo.zavar.brainstop.gateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.zavar.brainstop.auth.security.jwt.JwtTokenProvider;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.user.UserEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-1.1", "FR-1.3"})
@Layer(TestLayer.BE_UNIT)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "test-secret-key-must-be-long-enough-for-hs256-algorithm");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);

        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(Role.USER));
        testUser.setAvatarKey("avatar123.jpg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-JWT-01: Генерация токена с корректными claims")
    void generateToken_containsCorrectClaims() {
        String token = jwtTokenProvider.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        Claims claims = jwtTokenProvider.getClaims(token);
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("displayName", String.class)).isEqualTo("Test User");
        assertThat(claims.get("avatarKey", String.class)).isEqualTo("avatar123.jpg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-JWT-02: Валидация корректного токена")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken(testUser);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-JWT-03: Валидация некорректного токена")
    void validateToken_invalidToken_returnsFalse() {
        String invalidToken = "invalid.jwt.token";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-JWT-04: Роли сохраняются в токене")
    void generateToken_withMultipleRoles() {
        testUser.setRoles(Set.of(Role.USER, Role.ADMIN, Role.MODERATOR));

        String token = jwtTokenProvider.generateToken(testUser);
        Claims claims = jwtTokenProvider.getClaims(token);

        @SuppressWarnings("unchecked")
        java.util.List<String> roles = claims.get("roles", java.util.List.class);
        assertThat(roles).containsExactlyInAnyOrder("USER", "ADMIN", "MODERATOR");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-JWT-05: Токен без аватара генерируется корректно")
    void generateToken_noAvatar() {
        testUser.setAvatarKey(null);

        String token = jwtTokenProvider.generateToken(testUser);
        Claims claims = jwtTokenProvider.getClaims(token);

        assertThat(claims.get("avatarKey")).isNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-JWT-06: Парсинг claims из токена")
    void getClaims_extractsAllData() {
        String token = jwtTokenProvider.generateToken(testUser);

        Claims claims = jwtTokenProvider.getClaims(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("displayName", String.class)).isEqualTo("Test User");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }
}
