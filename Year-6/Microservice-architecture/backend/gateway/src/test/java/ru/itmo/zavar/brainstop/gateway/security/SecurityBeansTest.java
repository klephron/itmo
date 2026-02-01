package ru.itmo.zavar.brainstop.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import ru.itmo.zavar.brainstop.auth.security.SecurityBeans;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.user.UserEntity;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-1.1"})
@Layer(TestLayer.BE_UNIT)
class SecurityBeansTest {

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
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PRINCIPAL-01: UserPrincipal маппит данные пользователя")
    void userPrincipal_mapsUserData() {
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("testuser");
        assertThat(principal.getDisplayName()).isEqualTo("Test User");
        assertThat(principal.getPassword()).isEqualTo("encodedPassword");
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PRINCIPAL-02: Authorities содержат роли")
    void userPrincipal_authoritiesFromRoles() {
        testUser.setRoles(Set.of(Role.USER, Role.ADMIN));
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        assertThat(authorities).hasSize(2);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PRINCIPAL-03: AvatarUrl формируется из avatarKey")
    void userPrincipal_avatarUrl() {
        testUser.setAvatarKey("avatar123.jpg");
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        assertThat(principal.getAvatarUrl()).isEqualTo("/api/users/1/avatar");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PRINCIPAL-04: Без avatarKey возвращается null")
    void userPrincipal_noAvatarKey() {
        testUser.setAvatarKey(null);
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        assertThat(principal.getAvatarUrl()).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PRINCIPAL-05: getRoles возвращает роли пользователя")
    void userPrincipal_getRoles() {
        testUser.setRoles(Set.of(Role.MODERATOR));
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        assertThat(principal.getRoles()).containsExactly(Role.MODERATOR);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PRINCIPAL-06: Пустые роли возвращают пустой список authorities")
    void userPrincipal_emptyRoles() {
        testUser.setRoles(null);
        SecurityBeans.UserPrincipal principal = new SecurityBeans.UserPrincipal(testUser);

        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        assertThat(authorities).isEmpty();
    }
}
