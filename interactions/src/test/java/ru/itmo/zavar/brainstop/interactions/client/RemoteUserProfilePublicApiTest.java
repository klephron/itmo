package ru.itmo.zavar.brainstop.interactions.client;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.ProfileInternalClient;
import ru.itmo.zavar.brainstop.profile.client.RemoteUserProfilePublicApi;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1"})
@Layer(TestLayer.BE_UNIT)
class RemoteUserProfilePublicApiTest {

    @Mock
    private ProfileInternalClient profileInternalClient;

    @InjectMocks
    private RemoteUserProfilePublicApi remoteApi;

    private UserSummaryDto testSummary;

    @BeforeEach
    void setUp() {
        testSummary = new UserSummaryDto(100L, "Test User", "/avatar.jpg");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-API-01: Успешное получение summary")
    void findUserSummary_success() {
        when(profileInternalClient.getSummary(anyLong())).thenReturn(testSummary);

        Optional<UserSummaryDto> result = remoteApi.findUserSummary(100L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(100L);
        verify(profileInternalClient).getSummary(100L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROFILE-API-02: Пользователь не найден")
    void findUserSummary_notFound() {
        when(profileInternalClient.getSummary(anyLong()))
                .thenThrow(FeignException.NotFound.class);

        Optional<UserSummaryDto> result = remoteApi.findUserSummary(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROFILE-API-03: Circuit breaker открыт")
    void findUserSummary_circuitOpen() {
        when(profileInternalClient.getSummary(anyLong()))
                .thenThrow();

        Optional<UserSummaryDto> result = remoteApi.findUserSummary(100L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROFILE-API-04: Fallback недоступен")
    void findUserSummary_noFallback() {
        when(profileInternalClient.getSummary(anyLong()))
                .thenThrow(new NoFallbackAvailableException("test", new RuntimeException()));

        Optional<UserSummaryDto> result = remoteApi.findUserSummary(100L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROFILE-API-05: Ошибка сервиса (500)")
    void findUserSummary_serviceError() {
        when(profileInternalClient.getSummary(anyLong()))
                .thenThrow(mock(FeignException.InternalServerError.class));

        Optional<UserSummaryDto> result = remoteApi.findUserSummary(100L);

        assertThat(result).isEmpty();
    }
}
