package ru.itmo.zavar.brainstop.profile.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileAdminApi;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.profile.dto.FollowCheckRequest;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1", "FR-2.4"})
@Layer(TestLayer.BE_UNIT)
class ProfileInternalControllerTest {

    @Mock
    private UserProfilePublicApi userProfilePublicApi;

    @Mock
    private FollowQueryApi followQueryApi;

    @Mock
    private ProfileAdminApi profileAdminApi;

    @InjectMocks
    private ProfileInternalController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-CTL-01: Получение summary пользователя")
    void getSummary_success() {
        UserSummaryDto summary = new UserSummaryDto(100L, "Test User", "/avatar.jpg");
        when(userProfilePublicApi.findUserSummary(100L)).thenReturn(Optional.of(summary));

        ResponseEntity<UserSummaryDto> response = controller.getSummary(100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().displayName()).isEqualTo("Test User");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-INT-CTL-02: Summary несуществующего пользователя")
    void getSummary_notFound() {
        when(userProfilePublicApi.findUserSummary(999L)).thenReturn(Optional.empty());

        ResponseEntity<UserSummaryDto> response = controller.getSummary(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-CTL-03: Проверка подписок")
    void checkFollowing_success() {
        FollowCheckRequest request = new FollowCheckRequest(100L, Set.of(200L, 201L));
        when(followQueryApi.getFollowingWithin(100L, Set.of(200L, 201L)))
                .thenReturn(Set.of(200L));

        ResponseEntity<Set<Long>> response = controller.checkFollowing(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(200L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-CTL-04: Получение пользователя (админ)")
    void findUser_success() {
        ProfileUserDetails details = new ProfileUserDetails(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), null
        );
        when(profileAdminApi.findUser(100L)).thenReturn(Optional.of(details));

        ResponseEntity<ProfileUserDetails> response = controller.findUser(100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().username()).isEqualTo("user");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-CTL-05: Обновление ролей")
    void updateRoles_success() {
        ProfileUserDetails updated = new ProfileUserDetails(
                100L, "user", "User", "user@example.com",
                true, Set.of(Role.MODERATOR), LocalDateTime.now(), null
        );
        when(profileAdminApi.updateRoles(100L, Set.of(Role.MODERATOR))).thenReturn(updated);

        ResponseEntity<ProfileUserDetails> response = controller.updateRoles(100L, Set.of(Role.MODERATOR));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().roles()).contains(Role.MODERATOR);
    }
}
