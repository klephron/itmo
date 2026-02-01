package ru.itmo.zavar.brainstop.profile.controller;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.api.profile.UserAccountDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.profile.client.AuthInternalClient;
import ru.itmo.zavar.brainstop.profile.dto.PublicUserProfileDto;
import ru.itmo.zavar.brainstop.profile.service.FollowService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.1", "FR-2.2"})
@Layer(TestLayer.BE_UNIT)
class UserProfileControllerTest {

    @Mock
    private AuthInternalClient authInternalClient;

    @Mock
    private FollowService followService;

    @Mock
    private VideoProfileApi videoProfileApi;

    @InjectMocks
    private UserProfileController controller;

    private CurrentUser currentUser;
    private UserAccountDto testAccount;

    @BeforeEach
    void setUp() {
        currentUser = new CurrentUser(100L, "user", "User", null, Set.of(Role.USER));
        testAccount = new UserAccountDto(
                200L, "testuser", "Test User", "test@example.com",
                true, Set.of(Role.USER), LocalDateTime.now(), "avatar.jpg"
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-CTL-01: Получение профиля по ID")
    void getProfile_success() {
        when(authInternalClient.getUser(200L)).thenReturn(testAccount);
        when(followService.countFollowers(200L)).thenReturn(10L);
        when(followService.countFollowing(200L)).thenReturn(5L);
        when(followService.isFollowing(100L, 200L)).thenReturn(true);

        ResponseEntity<PublicUserProfileDto> response = controller.getProfile(200L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo("testuser");
        assertThat(response.getBody().followersCount()).isEqualTo(10L);
        assertThat(response.getBody().followingCount()).isEqualTo(5L);
        assertThat(response.getBody().following()).isTrue();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROFILE-CTL-02: Профиль несуществующего пользователя")
    void getProfile_notFound() {
        when(authInternalClient.getUser(999L)).thenThrow(FeignException.NotFound.class);

        ResponseEntity<PublicUserProfileDto> response = controller.getProfile(999L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-PROFILE-CTL-04: Видео несуществующего пользователя")
    void getUserVideos_userNotFound() {
        when(authInternalClient.exists(999L)).thenReturn(false);

        ResponseEntity<Page<PublicUserVideoDto>> response = controller.getUserVideos(
                999L, Pageable.unpaged(), currentUser
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROFILE-CTL-05: AvatarUrl формируется правильно")
    void getProfile_avatarUrl() {
        when(authInternalClient.getUser(200L)).thenReturn(testAccount);
        when(followService.countFollowers(anyLong())).thenReturn(0L);
        when(followService.countFollowing(anyLong())).thenReturn(0L);

        ResponseEntity<PublicUserProfileDto> response = controller.getProfile(200L, null);

        assertThat(response.getBody().avatarUrl()).isEqualTo("/api/users/200/avatar");
    }
}
