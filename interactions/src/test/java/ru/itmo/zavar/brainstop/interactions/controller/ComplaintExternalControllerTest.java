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
import ru.itmo.zavar.brainstop.interactions.dto.ComplaintDto;
import ru.itmo.zavar.brainstop.interactions.dto.CreateComplaintRequest;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintStatus;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintTargetType;
import ru.itmo.zavar.brainstop.interactions.service.ComplaintService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-7.1", "FR-7.2"})
@Layer(TestLayer.BE_UNIT)
class ComplaintExternalControllerTest {

    @Mock
    private ComplaintService complaintService;

    @InjectMocks
    private ComplaintExternalController controller;

    private UUID videoUuid;
    private CurrentUser user;
    private CurrentUser moderator;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        user = new CurrentUser(100L, "user", "User", null, Set.of(Role.USER));
        moderator = new CurrentUser(200L, "mod", "Moderator", null, Set.of(Role.MODERATOR));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-CTL-01: Пожаловаться на видео")
    void complainVideo_success() {
        CreateComplaintRequest request = new CreateComplaintRequest("Inappropriate");
        when(complaintService.submitVideoComplaint(any(), anyLong(), any())).thenReturn(UUID.randomUUID());

        ResponseEntity<Void> response = controller.complainVideo(videoUuid, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(complaintService).submitVideoComplaint(videoUuid, 100L, request);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-CTL-02: Жалоба без авторизации")
    void complainVideo_unauthorized() {
        CreateComplaintRequest request = new CreateComplaintRequest("Bad");

        ResponseEntity<Void> response = controller.complainVideo(videoUuid, request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(complaintService, never()).submitVideoComplaint(any(), anyLong(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-CTL-03: Пожаловаться на комментарий")
    void complainComment_success() {
        CreateComplaintRequest request = new CreateComplaintRequest("Spam");
        when(complaintService.submitCommentComplaint(any(), anyLong(), anyLong(), any())).thenReturn(UUID.randomUUID());

        ResponseEntity<Void> response = controller.complainComment(videoUuid, 1L, request, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(complaintService).submitCommentComplaint(videoUuid, 1L, 100L, request);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-CTL-04: Модератор получает список жалоб")
    void listOpen_asModerator() {
        ComplaintDto complaint = new ComplaintDto(
                UUID.randomUUID(), ComplaintTargetType.VIDEO, videoUuid, null,
                "Bad", 100L, ComplaintStatus.OPEN, null, null, Instant.now()
        );
        when(complaintService.listOpen()).thenReturn(List.of(complaint));

        ResponseEntity<List<ComplaintDto>> response = controller.listOpen(moderator);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(complaintService).listOpen();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-CTL-05: Обычный пользователь не может получить жалобы")
    void listOpen_asUser_forbidden() {
        ResponseEntity<List<ComplaintDto>> response = controller.listOpen(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(complaintService, never()).listOpen();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-CTL-06: Модератор закрывает жалобу")
    void resolve_asModerator() {
        UUID complaintId = UUID.randomUUID();
        doNothing().when(complaintService).resolve(any(), anyLong(), anyBoolean(), anyBoolean(), any());

        ResponseEntity<Void> response = controller.resolve(complaintId, false, false, "OK", moderator);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(complaintService).resolve(complaintId, 200L, false, false, "OK");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-CTL-07: Обычный пользователь не может закрыть жалобу")
    void resolve_asUser_forbidden() {
        UUID complaintId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.resolve(complaintId, false, false, null, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(complaintService, never()).resolve(any(), anyLong(), anyBoolean(), anyBoolean(), any());
    }
}
