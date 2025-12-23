package ru.itmo.zavar.brainstop.interactions.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.interactions.client.VideoAdminInternalClient;
import ru.itmo.zavar.brainstop.interactions.dto.ComplaintDto;
import ru.itmo.zavar.brainstop.interactions.dto.CreateComplaintRequest;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintEntity;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintStatus;
import ru.itmo.zavar.brainstop.interactions.model.ComplaintTargetType;
import ru.itmo.zavar.brainstop.interactions.repo.ComplaintRepository;
import ru.itmo.zavar.brainstop.comment.service.CommentService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CommentService commentService;
    private final VideoAdminInternalClient videoAdminInternalClient;

    @Transactional
    public UUID submitVideoComplaint(UUID videoUuid, Long authorId, CreateComplaintRequest req) {
        ComplaintEntity entity = new ComplaintEntity();
        entity.setId(UUID.randomUUID());
        entity.setTargetType(ComplaintTargetType.VIDEO);
        entity.setVideoUuid(videoUuid);
        entity.setReason(req.reason());
        entity.setAuthorId(authorId);
        entity.setStatus(ComplaintStatus.OPEN);
        return complaintRepository.save(entity).getId();
    }

    @Transactional
    public UUID submitCommentComplaint(UUID videoUuid, Long commentId, Long authorId, CreateComplaintRequest req) {
        ComplaintEntity entity = new ComplaintEntity();
        entity.setId(UUID.randomUUID());
        entity.setTargetType(ComplaintTargetType.COMMENT);
        entity.setVideoUuid(videoUuid);
        entity.setCommentId(commentId);
        entity.setReason(req.reason());
        entity.setAuthorId(authorId);
        entity.setStatus(ComplaintStatus.OPEN);
        return complaintRepository.save(entity).getId();
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto> listOpen() {
        return toDto(complaintRepository.findByStatusIn(List.of(ComplaintStatus.OPEN)));
    }

    @Transactional
    public void resolve(UUID id, Long moderatorId, boolean deleteTarget, boolean reject, String resolution) {
        ComplaintEntity entity = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + id));

        if (deleteTarget) {
            deleteTarget(entity, moderatorId);
        }

        entity.setStatus(reject ? ComplaintStatus.REJECTED : ComplaintStatus.RESOLVED);
        entity.setModeratorId(moderatorId);
        entity.setResolution(resolution);
        entity.setUpdatedAt(Instant.now());
        complaintRepository.save(entity);
    }

    private void deleteTarget(ComplaintEntity entity, Long moderatorId) {
        try {
            if (entity.getTargetType() == ComplaintTargetType.VIDEO && entity.getVideoUuid() != null) {
                videoAdminInternalClient.deleteVideo(entity.getVideoUuid());
            } else if (entity.getTargetType() == ComplaintTargetType.COMMENT
                    && entity.getVideoUuid() != null
                    && entity.getCommentId() != null) {
                // moderator=true to bypass ownership
                commentService.deleteComment(entity.getVideoUuid(), entity.getCommentId(), moderatorId, true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete target: " + e.getMessage(), e);
        }
    }

    private List<ComplaintDto> toDto(List<ComplaintEntity> list) {
        return list.stream()
                .map(e -> new ComplaintDto(
                        e.getId(),
                        e.getTargetType(),
                        e.getVideoUuid(),
                        e.getCommentId(),
                        e.getReason(),
                        e.getAuthorId(),
                        e.getStatus(),
                        e.getModeratorId(),
                        e.getResolution(),
                        e.getCreatedAt()
                ))
                .toList();
    }
}
