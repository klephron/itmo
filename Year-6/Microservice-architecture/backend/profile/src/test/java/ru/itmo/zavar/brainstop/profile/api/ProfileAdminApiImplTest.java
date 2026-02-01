package ru.itmo.zavar.brainstop.profile.api;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.5"})
@Layer(TestLayer.BE_UNIT)
class ProfileAdminApiImplTest {

    @Mock
    private AuthInternalClient authInternalClient;

    @InjectMocks
    private ProfileAdminApiImpl profileAdminApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-API-01: Получение всех пользователей")
    void findAllUsers_success() {
        UserAccountDto user = new UserAccountDto(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), null
        );
        when(authInternalClient.listUsers()).thenReturn(List.of(user));

        List<ProfileUserDetails> result = profileAdminApi.findAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("user");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-API-02: Получение пользователя по ID")
    void findUser_success() {
        UserAccountDto user = new UserAccountDto(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar.jpg"
        );
        when(authInternalClient.getUser(100L)).thenReturn(user);

        Optional<ProfileUserDetails> result = profileAdminApi.findUser(100L);

        assertThat(result).isPresent();
        assertThat(result.get().avatarUrl()).isEqualTo("/api/users/100/avatar");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ADMIN-API-03: Пользователь не найден")
    void findUser_notFound() {
        when(authInternalClient.getUser(999L)).thenThrow(FeignException.NotFound.class);

        Optional<ProfileUserDetails> result = profileAdminApi.findUser(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-API-04: Проверка существования")
    void existsById_true() {
        when(authInternalClient.exists(100L)).thenReturn(true);

        boolean result = profileAdminApi.existsById(100L);

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-API-05: Установка enabled")
    void setEnabled_success() {
        UserAccountDto updated = new UserAccountDto(
                100L, "user", "User", "user@example.com",
                false, Set.of(Role.USER), LocalDateTime.now(), null
        );
        when(authInternalClient.setEnabled(100L, false)).thenReturn(updated);

        ProfileUserDetails result = profileAdminApi.setEnabled(100L, false);

        assertThat(result.enabled()).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-API-06: Обновление ролей")
    void updateRoles_success() {
        UserAccountDto updated = new UserAccountDto(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.MODERATOR), LocalDateTime.now(), null
        );
        when(authInternalClient.updateRoles(100L, Set.of(Role.MODERATOR))).thenReturn(updated);

        ProfileUserDetails result = profileAdminApi.updateRoles(100L, Set.of(Role.MODERATOR));

        assertThat(result.roles()).contains(Role.MODERATOR);
    }
}
