package ru.itmo.zavar.brainstop.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.admin.client.ProfileAdminClient;
import ru.itmo.zavar.brainstop.admin.client.VideoModerationClient;
import ru.itmo.zavar.brainstop.admin.controller.ModerationController;
import ru.itmo.zavar.brainstop.admin.dto.UserDetailDto;
import ru.itmo.zavar.brainstop.admin.mapper.ProfileModerationMapper;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-14.2", "FR-14.3"})
@Layer(TestLayer.BE_UNIT)
class ModerationControllerTest {

    @Mock
    private ProfileAdminClient profileAdminClient;

    @Mock
    private VideoModerationClient videoModerationClient;

    @Mock
    private ProfileModerationMapper profileModerationMapper;

    @InjectMocks
    private ModerationController moderationController;

    private CurrentUser moderatorUser;
    private ProfileUserDetails testUser;

    @BeforeEach
    void setUp() {
        moderatorUser = new CurrentUser(
            1L,
            "moderator",
            "Moderator User",
            null,
            Set.of(Role.MODERATOR)
        );

        testUser = new ProfileUserDetails(
                2L, "user1", "User One", "user1@test.com",
                true, Set.of(Role.USER), LocalDateTime.now(), null
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-01: Получение списка пользователей")
    void listUsers_success() {
        UserDetailDto userDto = new UserDetailDto(
                2L, "user1", "User One", "user1@test.com",
                true, Set.of(Role.USER), LocalDateTime.now()
        );

        when(profileAdminClient.findAllUsers()).thenReturn(List.of(testUser));
        when(profileModerationMapper.toDto(any(ProfileUserDetails.class))).thenReturn(userDto);

        List<UserDetailDto> result = moderationController.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("user1");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-02: Получение деталей пользователя")
    void getUser_success() {
        UserDetailDto userDto = new UserDetailDto(
                2L, "user1", "User One", "user1@test.com",
                true, Set.of(Role.USER), LocalDateTime.now()
        );

        when(profileAdminClient.findUser(anyLong())).thenReturn(testUser);
        when(profileModerationMapper.toDto(any(ProfileUserDetails.class))).thenReturn(userDto);

        ResponseEntity<UserDetailDto> response = moderationController.getUser(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo("user1");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-MOD-03: Получение несуществующего пользователя")
    void getUser_notFound() {
        when(profileAdminClient.findUser(anyLong())).thenReturn(null);

        ResponseEntity<UserDetailDto> response = moderationController.getUser(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-04: Получение видео пользователя")
    void getUserVideos_success() {
        when(profileAdminClient.exists(anyLong())).thenReturn(true);
        when(videoModerationClient.getUserVideos(anyLong())).thenReturn(List.of());

        ResponseEntity<List<UserVideoDto>> response = moderationController.getUserVideos(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-MOD-05: Получение видео несуществующего пользователя")
    void getUserVideos_userNotFound() {
        when(profileAdminClient.exists(anyLong())).thenReturn(false);

        ResponseEntity<List<UserVideoDto>> response = moderationController.getUserVideos(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-06: Бан пользователя")
    void banUser_success() {
        ProfileUserDetails bannedUser = new ProfileUserDetails(
                2L, "user1", "User One", "user1@test.com",
                false, Set.of(Role.USER), LocalDateTime.now(), null
        );

        when(profileAdminClient.setEnabled(anyLong(), anyBoolean())).thenReturn(bannedUser);

        ResponseEntity<Void> response = moderationController.banUser(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-MOD-07: Разбан пользователя")
    void unbanUser_success() {
        when(profileAdminClient.setEnabled(anyLong(), anyBoolean())).thenReturn(testUser);

        ResponseEntity<Void> response = moderationController.unbanUser(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-MOD-08: Бан несуществующего пользователя")
    void banUser_notFound() {
        when(profileAdminClient.setEnabled(anyLong(), anyBoolean())).thenReturn(null);

        ResponseEntity<Void> response = moderationController.banUser(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
