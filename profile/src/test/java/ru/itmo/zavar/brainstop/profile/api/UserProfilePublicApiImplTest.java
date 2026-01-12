package ru.itmo.zavar.brainstop.profile.api;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.cache.UserSummaryCache;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class UserProfilePublicApiImplTest {

    @Mock
    private AuthInternalClient authInternalClient;

    @Mock
    private UserSummaryCache userSummaryCache;

    @InjectMocks
    private UserProfilePublicApiImpl userProfilePublicApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROF-API-01: Получение summary из кеша")
    void findUserSummary_fromCache() {
        UserSummaryDto cached = new UserSummaryDto(100L, "User", "/avatar.jpg");
        when(userSummaryCache.get(100L)).thenReturn(Optional.of(cached));

        Optional<UserSummaryDto> result = userProfilePublicApi.findUserSummary(100L);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("User");
        verify(authInternalClient, never()).getUser(anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROF-API-02: Получение summary из клиента")
    void findUserSummary_fromClient() {
        UserAccountDto account = new UserAccountDto(
                100L, "testuser", "Test User", "test@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar.jpg"
        );

        when(userSummaryCache.get(100L)).thenReturn(Optional.empty());
        when(authInternalClient.getUser(100L)).thenReturn(account);

        Optional<UserSummaryDto> result = userProfilePublicApi.findUserSummary(100L);

        assertThat(result).isPresent();
        assertThat(result.get().displayName()).isEqualTo("Test User");
        assertThat(result.get().avatarUrl()).isEqualTo("/api/users/100/avatar");
        verify(userSummaryCache).put(any(UserSummaryDto.class));
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROF-API-03: Пользователь не найден")
    void findUserSummary_notFound() {
        when(userSummaryCache.get(999L)).thenReturn(Optional.empty());
        when(authInternalClient.getUser(999L)).thenThrow(FeignException.NotFound.class);

        Optional<UserSummaryDto> result = userProfilePublicApi.findUserSummary(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROF-API-04: Обработка null avatarKey")
    void findUserSummary_nullAvatar() {
        UserAccountDto account = new UserAccountDto(
                100L, "testuser", "Test User", "test@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), null
        );

        when(userSummaryCache.get(100L)).thenReturn(Optional.empty());
        when(authInternalClient.getUser(100L)).thenReturn(account);

        Optional<UserSummaryDto> result = userProfilePublicApi.findUserSummary(100L);

        assertThat(result).isPresent();
        assertThat(result.get().avatarUrl()).isNull();
    }
}
