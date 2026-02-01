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
import ru.itmo.zavar.brainstop.admin.client.VideoAdminClient;
import ru.itmo.zavar.brainstop.admin.controller.AdminController;
import ru.itmo.zavar.brainstop.admin.dto.UpdateUserRolesRequest;
import ru.itmo.zavar.brainstop.common.api.profile.ProfileUserDetails;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-14.2", "FR-14.3"})
@Layer(TestLayer.BE_UNIT)
class AdminControllerTest {

    @Mock
    private VideoAdminClient videoAdminClient;

    @Mock
    private ProfileAdminClient profileAdminClient;

    @InjectMocks
    private AdminController adminController;

    private CurrentUser adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new CurrentUser(
            1L,
            "admin",
            "Admin User",
            null,
            Set.of(Role.ADMIN)
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-01: Успешная регенерация превью")
    void regenerateThumbnails_success() {
        when(videoAdminClient.regenerateThumbnails(anyBoolean()))
                .thenReturn(Map.of("updated", 5, "force", true));

        ResponseEntity<Map<String, Object>> response = adminController.regenerateThumbnails(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("updated")).isEqualTo(5);
        assertThat(response.getBody().get("force")).isEqualTo(true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-02: Регенерация превью возвращает unavailable при сбое")
    void regenerateThumbnails_serviceUnavailable() {
        when(videoAdminClient.regenerateThumbnails(anyBoolean()))
                .thenReturn(Map.of("status", "unavailable", "message", "video service not available"));

        ResponseEntity<Map<String, Object>> response = adminController.regenerateThumbnails(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("unavailable");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-03: Получение ролей пользователя")
    void getUserRoles_success() {
        ProfileUserDetails userDetails = new ProfileUserDetails(
                2L, "user", "User", "user@test.com", true,
                Set.of(Role.USER, Role.MODERATOR), null, null
        );

        when(profileAdminClient.findUser(anyLong())).thenReturn(userDetails);

        ResponseEntity<Set<Role>> response = adminController.getUserRoles(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(Role.USER, Role.MODERATOR);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ADMIN-04: Получение ролей несуществующего пользователя")
    void getUserRoles_userNotFound() {
        when(profileAdminClient.findUser(anyLong())).thenReturn(null);

        ResponseEntity<Set<Role>> response = adminController.getUserRoles(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-05: Обновление ролей пользователя")
    void updateUserRoles_success() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest(Set.of(Role.USER, Role.MODERATOR));
        ProfileUserDetails updated = new ProfileUserDetails(
                2L, "user", "User", "user@test.com", true,
                Set.of(Role.USER, Role.MODERATOR), null, null
        );

        when(profileAdminClient.updateRoles(eq(2L), eq(request.roles()))).thenReturn(updated);

        ResponseEntity<Set<Role>> response = adminController.updateUserRoles(2L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyInAnyOrder(Role.USER, Role.MODERATOR);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-ADMIN-06: Обновление ролей несуществующего пользователя")
    void updateUserRoles_userNotFound() {
        UpdateUserRolesRequest request = new UpdateUserRolesRequest(Set.of(Role.USER));

        when(profileAdminClient.updateRoles(anyLong(), eq(request.roles()))).thenReturn(null);

        ResponseEntity<Set<Role>> response = adminController.updateUserRoles(999L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-ADMIN-07: Регенерация с force=false")
    void regenerateThumbnails_withoutForce() {
        when(videoAdminClient.regenerateThumbnails(false))
                .thenReturn(Map.of("updated", 2, "force", false));

        ResponseEntity<Map<String, Object>> response = adminController.regenerateThumbnails(false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("updated")).isEqualTo(2);
        assertThat(response.getBody().get("force")).isEqualTo(false);
    }
}
