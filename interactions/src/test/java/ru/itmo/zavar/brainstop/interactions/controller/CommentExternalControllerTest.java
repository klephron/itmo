package ru.itmo.zavar.brainstop.interactions.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.comment.service.CommentService;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-6.1", "FR-6.2"})
@Layer(TestLayer.BE_UNIT)
class CommentExternalControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentExternalController controller;

    private UUID videoUuid;
    private CurrentUser currentUser;
    private CommentDto testComment;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        currentUser = new CurrentUser(100L, "testuser", "Test User", "/avatar.jpg", Set.of(Role.USER));
        testComment = new CommentDto(1L, 100L, "Test Author", "/avatar.jpg", "Great video!", Instant.now(), true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-CTL-01: Получение списка комментариев")
    void list_success() {
        Page<CommentDto> page = new PageImpl<>(List.of(testComment));
        when(commentService.listComments(any(UUID.class), anyLong(), any())).thenReturn(page);

        ResponseEntity<Page<CommentDto>> response = controller.list(videoUuid, 0, 20, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(commentService).listComments(eq(videoUuid), eq(100L), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-CTL-02: Получение комментариев анонимом")
    void list_anonymous() {
        Page<CommentDto> page = new PageImpl<>(List.of(testComment));
        when(commentService.listComments(any(UUID.class), any(), any())).thenReturn(page);

        ResponseEntity<Page<CommentDto>> response = controller.list(videoUuid, 0, 20, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commentService).listComments(eq(videoUuid), eq(null), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-CTL-03: Добавление комментария")
    void add_success() {
        CreateCommentRequest request = new CreateCommentRequest("Nice video");
        when(commentService.addComment(any(), any(), anyLong(), anyString(), anyString())).thenReturn(testComment);

        ResponseEntity<CommentDto> response = controller.add(videoUuid, request, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMM-CTL-04: Добавление комментария без авторизации")
    void add_unauthorized() {
        CreateCommentRequest request = new CreateCommentRequest("Nice video");

        ResponseEntity<CommentDto> response = controller.add(videoUuid, request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(commentService, never()).addComment(any(), any(), anyLong(), anyString(), anyString());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-CTL-05: Удаление комментария")
    void delete_success() {
        doNothing().when(commentService).deleteComment(any(), anyLong(), anyLong(), anyBoolean());

        ResponseEntity<Void> response = controller.delete(videoUuid, 1L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(commentService).deleteComment(videoUuid, 1L, 100L, false);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMM-CTL-06: Удаление комментария без авторизации")
    void delete_unauthorized() {
        ResponseEntity<Void> response = controller.delete(videoUuid, 1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(commentService, never()).deleteComment(any(), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMM-CTL-07: Модератор удаляет комментарий")
    void delete_asModerator() {
        CurrentUser moderator = new CurrentUser(200L, "mod", "Moderator", null, Set.of(Role.MODERATOR));
        doNothing().when(commentService).deleteComment(any(), anyLong(), anyLong(), anyBoolean());

        ResponseEntity<Void> response = controller.delete(videoUuid, 1L, moderator);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(commentService).deleteComment(videoUuid, 1L, 200L, true);
    }
}
