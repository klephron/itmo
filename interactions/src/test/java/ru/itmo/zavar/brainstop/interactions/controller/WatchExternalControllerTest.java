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
import ru.itmo.zavar.brainstop.like.dto.request.WatchProgressRequest;
import ru.itmo.zavar.brainstop.like.service.WatchService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.4"})
@Layer(TestLayer.BE_UNIT)
class WatchExternalControllerTest {

    @Mock
    private WatchService watchService;

    @InjectMocks
    private WatchExternalController controller;

    private UUID videoUuid;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        currentUser = new CurrentUser(100L, "testuser", "Test User", null, Set.of(Role.USER));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-CTL-01: Записать прогресс авторизованным пользователем")
    void watch_authenticated() {
        WatchProgressRequest request = new WatchProgressRequest(30L, 100L, null);
        doNothing().when(watchService).recordProgress(any(), anyLong(), any());

        ResponseEntity<Void> response = controller.watch(videoUuid, currentUser, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(watchService).recordProgress(videoUuid, 100L, request);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-CTL-02: Записать прогресс анонимом")
    void watch_anonymous() {
        WatchProgressRequest request = new WatchProgressRequest(30L, 100L, null);
        doNothing().when(watchService).recordProgress(any(), any(), any());

        ResponseEntity<Void> response = controller.watch(videoUuid, null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(watchService).recordProgress(videoUuid, null, request);
    }
}
