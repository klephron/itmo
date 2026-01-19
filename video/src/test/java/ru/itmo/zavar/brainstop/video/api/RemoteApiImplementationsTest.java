package ru.itmo.zavar.brainstop.video.api;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.profile.dto.FollowCheckRequest;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.ProfileInternalClient;
import ru.itmo.zavar.brainstop.profile.client.RemoteFollowQueryApi;
import ru.itmo.zavar.brainstop.profile.client.RemoteProfileAdminApi;
import ru.itmo.zavar.brainstop.profile.client.RemoteUserProfilePublicApi;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-4.1"})
@Layer(TestLayer.BE_UNIT)
class RemoteApiImplementationsTest {

    @Mock
    private ProfileInternalClient profileInternalClient;

    @InjectMocks
    private RemoteProfileAdminApi profileAdminApi;

    @InjectMocks
    private RemoteFollowQueryApi followQueryApi;

    @InjectMocks
    private RemoteUserProfilePublicApi userProfilePublicApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-01: RemoteProfileAdminApi.findAllUsers")
    void remoteProfileAdmin_findAllUsers() {
        ProfileUserDetails user = new ProfileUserDetails(
                1L, "user", "User", "email", true, Set.of(Role.USER), null, ""
        );
        when(profileInternalClient.findAllUsers()).thenReturn(List.of(user));

        List<ProfileUserDetails> result = profileAdminApi.findAllUsers();

        assertThat(result).hasSize(1);
        verify(profileInternalClient).findAllUsers();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-02: RemoteProfileAdminApi.findUser возвращает user")
    void remoteProfileAdmin_findUser() {
        ProfileUserDetails user = new ProfileUserDetails(
                1L, "user", "User", "email", true, Set.of(Role.USER), null, ""
        );
        when(profileInternalClient.findUser(1L)).thenReturn(user);

        Optional<ProfileUserDetails> result = profileAdminApi.findUser(1L);

        assertThat(result).isPresent();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-REMOTE-03: RemoteProfileAdminApi.findUser обрабатывает NotFound")
    void remoteProfileAdmin_findUser_notFound() {
        when(profileInternalClient.findUser(anyLong())).thenThrow(FeignException.NotFound.class);

        Optional<ProfileUserDetails> result = profileAdminApi.findUser(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-04: RemoteProfileAdminApi.existsById true")
    void remoteProfileAdmin_existsById_true() {
        when(profileInternalClient.exists(1L)).thenReturn(true);

        boolean result = profileAdminApi.existsById(1L);

        assertThat(result).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-05: RemoteProfileAdminApi.existsById false")
    void remoteProfileAdmin_existsById_false() {
        when(profileInternalClient.exists(1L)).thenReturn(false);

        boolean result = profileAdminApi.existsById(1L);

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-06: RemoteProfileAdminApi.existsById null")
    void remoteProfileAdmin_existsById_null() {
        when(profileInternalClient.exists(1L)).thenReturn(null);

        boolean result = profileAdminApi.existsById(1L);

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-REMOTE-08: RemoteFollowQueryApi с null followerId")
    void remoteFollowQuery_nullFollowerId() {
        Set<Long> result = followQueryApi.getFollowingWithin(null, Set.of(1L, 2L));

        assertThat(result).isEmpty();
        verify(profileInternalClient, never()).checkFollowing(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-REMOTE-09: RemoteFollowQueryApi с пустыми candidates")
    void remoteFollowQuery_emptyCandidates() {
        Set<Long> result = followQueryApi.getFollowingWithin(1L, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-10: RemoteUserProfilePublicApi.findUserSummary")
    void remoteUserProfile_findUserSummary() {
        UserSummaryDto summary = new UserSummaryDto(1L, "User", "avatar.jpg");
        when(profileInternalClient.getSummary(1L)).thenReturn(summary);

        Optional<UserSummaryDto> result = userProfilePublicApi.findUserSummary(1L);

        assertThat(result).hasValue(summary);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-REMOTE-11: RemoteUserProfilePublicApi обрабатывает NotFound")
    void remoteUserProfile_notFound() {
        when(profileInternalClient.getSummary(anyLong())).thenThrow(FeignException.NotFound.class);

        Optional<UserSummaryDto> result = userProfilePublicApi.findUserSummary(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-12: RemoteProfileAdminApi.setEnabled")
    void remoteProfileAdmin_setEnabled() {
        ProfileUserDetails user = new ProfileUserDetails(
                1L, "user", "User", "email", true, Set.of(Role.USER), null, ""
        );
        when(profileInternalClient.setEnabled(1L, false)).thenReturn(user);

        ProfileUserDetails result = profileAdminApi.setEnabled(1L, false);

        assertThat(result).isEqualTo(user);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REMOTE-13: RemoteProfileAdminApi.updateRoles")
    void remoteProfileAdmin_updateRoles() {
        ProfileUserDetails user = new ProfileUserDetails(
                1L, "user", "User", "email", true, Set.of(Role.ADMIN), null, ""
        );
        when(profileInternalClient.updateRoles(1L, Set.of(Role.ADMIN))).thenReturn(user);

        ProfileUserDetails result = profileAdminApi.updateRoles(1L, Set.of(Role.ADMIN));

        assertThat(result.roles()).contains(Role.ADMIN);
    }
}
