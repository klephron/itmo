package ru.itmo.zavar.brainstop.interactions.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.comment.service.CommentService;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.client.VideoAdminInternalClient;
import ru.itmo.zavar.brainstop.interactions.dto.ComplaintDto;
import ru.itmo.zavar.brainstop.interactions.dto.CreateComplaintRequest;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintEntity;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintStatus;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintTargetType;
import ru.itmo.zavar.brainstop.interactions.repo.ComplaintRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-7.1", "FR-7.2"})
@Layer(TestLayer.BE_UNIT)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private VideoAdminInternalClient videoAdminInternalClient;

    @InjectMocks
    private ComplaintService complaintService;

    private ComplaintEntity testComplaint;
    private UUID videoUuid;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        testComplaint = new ComplaintEntity();
        testComplaint.setId(UUID.randomUUID());
        testComplaint.setTargetType(ComplaintTargetType.VIDEO);
        testComplaint.setVideoUuid(videoUuid);
        testComplaint.setReason("Inappropriate content");
        testComplaint.setAuthorId(100L);
        testComplaint.setStatus(ComplaintStatus.OPEN);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-01: Создание жалобы на видео")
    void submitVideoComplaint_success() {
        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(testComplaint);
        CreateComplaintRequest request = new CreateComplaintRequest("Bad video");

        UUID id = complaintService.submitVideoComplaint(videoUuid, 100L, request);

        assertThat(id).isNotNull();
        verify(complaintRepository).save(any(ComplaintEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-02: Создание жалобы на комментарий")
    void submitCommentComplaint_success() {
        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(testComplaint);
        CreateComplaintRequest request = new CreateComplaintRequest("Spam comment");

        UUID id = complaintService.submitCommentComplaint(videoUuid, 1L, 100L, request);

        assertThat(id).isNotNull();
        verify(complaintRepository).save(any(ComplaintEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-03: Получение списка открытых жалоб")
    void listOpen_success() {
        when(complaintRepository.findByStatusIn(any())).thenReturn(List.of(testComplaint));

        List<ComplaintDto> result = complaintService.listOpen();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ComplaintStatus.OPEN);
        assertThat(result.get(0).targetType()).isEqualTo(ComplaintTargetType.VIDEO);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-04: Закрытие жалобы без удаления")
    void resolve_withoutDelete() {
        when(complaintRepository.findById(any(UUID.class))).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(testComplaint);

        complaintService.resolve(testComplaint.getId(), 200L, false, false, "Resolved");

        assertThat(testComplaint.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(testComplaint.getModeratorId()).isEqualTo(200L);
        assertThat(testComplaint.getResolution()).isEqualTo("Resolved");
        verify(complaintRepository).save(testComplaint);
        verify(videoAdminInternalClient, never()).deleteVideo(any());
        verify(commentService, never()).deleteComment(any(), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-05: Закрытие жалобы с удалением видео")
    void resolve_withDeleteVideo() {
        when(complaintRepository.findById(any(UUID.class))).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(testComplaint);
        doNothing().when(videoAdminInternalClient).deleteVideo(any(UUID.class));

        complaintService.resolve(testComplaint.getId(), 200L, true, false, "Video deleted");

        assertThat(testComplaint.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
        verify(videoAdminInternalClient).deleteVideo(videoUuid);
        verify(complaintRepository).save(testComplaint);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-06: Отклонение жалобы")
    void resolve_reject() {
        when(complaintRepository.findById(any(UUID.class))).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(testComplaint);

        complaintService.resolve(testComplaint.getId(), 200L, false, true, "Not a violation");

        assertThat(testComplaint.getStatus()).isEqualTo(ComplaintStatus.REJECTED);
        assertThat(testComplaint.getModeratorId()).isEqualTo(200L);
        verify(complaintRepository).save(testComplaint);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-COMPL-07: Закрытие жалобы с удалением комментария")
    void resolve_withDeleteComment() {
        testComplaint.setTargetType(ComplaintTargetType.COMMENT);
        testComplaint.setCommentId(5L);

        when(complaintRepository.findById(any(UUID.class))).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(ComplaintEntity.class))).thenReturn(testComplaint);
        doNothing().when(commentService).deleteComment(any(), anyLong(), anyLong(), anyBoolean());

        complaintService.resolve(testComplaint.getId(), 200L, true, false, "Comment deleted");

        verify(commentService).deleteComment(videoUuid, 5L, 200L, true);
        verify(complaintRepository).save(testComplaint);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-COMPL-08: Закрытие несуществующей жалобы")
    void resolve_notFound_throws() {
        UUID complaintId = UUID.randomUUID();
        when(complaintRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.resolve(complaintId, 200L, false, false, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Complaint not found");
    }
}
