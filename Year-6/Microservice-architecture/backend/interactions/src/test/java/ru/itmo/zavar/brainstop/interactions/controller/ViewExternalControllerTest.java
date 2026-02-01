package ru.itmo.zavar.brainstop.interactions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.like.dto.response.ViewResponse;
import ru.itmo.zavar.brainstop.like.service.ViewService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.3"})
@Layer(TestLayer.BE_UNIT)
class ViewExternalControllerTest {

    @Mock
    private ViewService viewService;

    @InjectMocks
    private ViewExternalController controller;

    private UUID videoUuid;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        currentUser = new CurrentUser(100L, "testuser", "Test User", null, Set.of(Role.USER));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIEW-CTL-01: Отметить просмотр авторизованным пользователем")
    void view_authenticated() {
        when(viewService.viewVideo(any(UUID.class), anyLong())).thenReturn(101L);

        ResponseEntity<ViewResponse> response = controller.view(videoUuid, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().viewsCount()).isEqualTo(101L);
        verify(viewService).viewVideo(videoUuid, 100L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIEW-CTL-02: Отметить просмотр анонимом")
    void view_anonymous() {
        when(viewService.viewVideo(any(UUID.class), any())).thenReturn(102L);

        ResponseEntity<ViewResponse> response = controller.view(videoUuid, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().viewsCount()).isEqualTo(102L);
        verify(viewService).viewVideo(videoUuid, null);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIEW-CTL-03: Получение количества просмотров")
    void getViews_success() {
        when(viewService.getViews(any(UUID.class))).thenReturn(250L);

        ResponseEntity<ViewResponse> response = controller.getViews(videoUuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().viewsCount()).isEqualTo(250L);
        verify(viewService).getViews(videoUuid);
    }
}
