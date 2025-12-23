package ru.itmo.zavar.brainstop.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.comment.model.CommentEntity;
import ru.itmo.zavar.brainstop.comment.repo.CommentRepository;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
// MSPLIT: boundary for future Comment Service API
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoMetadataPublicApi videoMetadataPublicApi;
    private final UserProfilePublicApi userProfilePublicApi;

    @Transactional(readOnly = true)
    public List<CommentDto> listComments(UUID videoUuid, Long currentUserId) {
        this.videoMetadataPublicApi.getRequired(videoUuid);

        List<CommentEntity> comments =
                this.commentRepository.findByVideoUuidOrderByCreatedAtAsc(videoUuid);

        return comments.stream()
                .map(c -> {
                    UserSummaryDto user = c.getAuthorId() == null
                            ? null
                            : this.userProfilePublicApi.findUserSummary(c.getAuthorId()).orElse(null);
                    return new CommentDto(
                            c.getId(),
                            c.getAuthorId(),
                            c.getAuthorName(),
                            user != null ? user.avatarUrl() : null,
                            c.getText(),
                            c.getCreatedAt(),
                            Objects.equals(currentUserId, c.getAuthorId())
                    );
                })
                .toList();
    }

    @Transactional
    public CommentDto addComment(UUID videoUuid,
                                 CreateCommentRequest req,
                                 Long currentUserId,
                                 String displayName,
                                 String avatarUrl) {

        this.videoMetadataPublicApi.getRequired(videoUuid);

        CommentEntity entity = new CommentEntity();
        entity.setVideoUuid(videoUuid);
        entity.setAuthorId(currentUserId);
        entity.setAuthorName(displayName);
        entity.setText(req.text());
        entity.setCreatedAt(Instant.now());

        entity = this.commentRepository.save(entity);

        return new CommentDto(
                entity.getId(),
                entity.getAuthorId(),
                entity.getAuthorName(),
                avatarUrl,
                entity.getText(),
                entity.getCreatedAt(),
                true
        );
    }

    @Transactional
    public void deleteComment(UUID videoUuid, Long commentId, Long currentUserId, boolean isModeratorOrAdmin) {
        CommentEntity entity = this.commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!entity.getVideoUuid().equals(videoUuid)) {
            throw new IllegalArgumentException("Comment does not belong to this video");
        }

        if (!isModeratorOrAdmin && !entity.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Forbidden");
        }

        this.commentRepository.delete(entity);
    }
}
