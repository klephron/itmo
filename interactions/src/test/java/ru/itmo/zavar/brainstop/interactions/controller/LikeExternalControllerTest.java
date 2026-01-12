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
import ru.itmo.zavar.brainstop.like.dto.response.LikeResponse;
import ru.itmo.zavar.brainstop.like.service.LikeService;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class LikeExternalControllerTest {

    @Mock
    private LikeService likeService;

    @InjectMocks
    private LikeExternalController controller;

    private UUID videoUuid;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        currentUser = new CurrentUser(100L, "testuser", "Test User", null, Set.of(Role.USER));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-CTL-01: Поставить лайк")
    void like_success() {
        when(likeService.likeVideo(any(UUID.class), anyLong())).thenReturn(6L);

        ResponseEntity<LikeResponse> response = controller.like(videoUuid, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().likesCount()).isEqualTo(6L);
        assertThat(response.getBody().liked()).isTrue();
        verify(likeService).likeVideo(videoUuid, 100L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-CTL-02: Лайк без авторизации")
    void like_unauthorized() {
        ResponseEntity<LikeResponse> response = controller.like(videoUuid, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(likeService, never()).likeVideo(any(), anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-CTL-03: Снять лайк")
    void unlike_success() {
        when(likeService.unlikeVideo(any(UUID.class), anyLong())).thenReturn(5L);

        ResponseEntity<LikeResponse> response = controller.unlike(videoUuid, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().likesCount()).isEqualTo(5L);
        assertThat(response.getBody().liked()).isFalse();
        verify(likeService).unlikeVideo(videoUuid, 100L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-CTL-04: Анлайк без авторизации")
    void unlike_unauthorized() {
        ResponseEntity<LikeResponse> response = controller.unlike(videoUuid, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(likeService, never()).unlikeVideo(any(), anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-CTL-05: Получение статуса лайка")
    void getLikeStatus_success() {
        when(likeService.isLikedByUser(any(UUID.class), anyLong())).thenReturn(true);

        ResponseEntity<LikeResponse> response = controller.getLikeStatus(videoUuid, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().liked()).isTrue();
        verify(likeService).isLikedByUser(videoUuid, 100L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-CTL-06: Статус лайка без авторизации")
    void getLikeStatus_unauthorized() {
        ResponseEntity<LikeResponse> response = controller.getLikeStatus(videoUuid, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(likeService, never()).isLikedByUser(any(), anyLong());
    }
}
