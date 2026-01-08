package ru.itmo.zavar.brainstop.comment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.comment.model.CommentEntity;
import ru.itmo.zavar.brainstop.comment.repo.CommentRepository;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserSummaryDto;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
// MSPLIT: boundary for future Comment Service API
@RequiredArgsConstructor
public class CommentService {

    private static final Duration COMMENTS_TTL = Duration.ofSeconds(30);
    private static final String COMMENTS_KEY_PREFIX = "comments:video:";

    private final CommentRepository commentRepository;
    private final VideoMetadataPublicApi videoMetadataPublicApi;
    private final UserProfilePublicApi userProfilePublicApi;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CommentDto> listComments(UUID videoUuid, Long currentUserId) {
        this.videoMetadataPublicApi.getRequired(videoUuid);

        List<CommentDto> cached = readCommentsCache(videoUuid);
        if (cached != null) {
            return applyMine(cached, currentUserId);
        }

        Page<CommentDto> page = listComments(videoUuid, currentUserId, Pageable.unpaged());
        writeCommentsCache(videoUuid, page.getContent());
        return page.getContent();
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> listComments(UUID videoUuid, Long currentUserId, Pageable pageable) {
        this.videoMetadataPublicApi.getRequired(videoUuid);

        Pageable ordered = pageable == null
                ? PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt"))
                : PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.max(1, pageable.getPageSize()),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        Page<CommentEntity> page = this.commentRepository.findByVideoUuid(videoUuid, ordered);

        List<CommentDto> dtos = page.getContent().stream()
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

        return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
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
        evictCommentsCache(videoUuid);

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
        evictCommentsCache(videoUuid);
    }

    private List<CommentDto> readCommentsCache(UUID videoUuid) {
        if (videoUuid == null) {
            return null;
        }
        String payload = redisTemplate.opsForValue().get(commentsKey(videoUuid));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<List<CommentDto>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void writeCommentsCache(UUID videoUuid, List<CommentDto> comments) {
        if (videoUuid == null || comments == null) {
            return;
        }
        List<CommentDto> payload = comments.stream()
                .map(c -> new CommentDto(
                        c.id(),
                        c.authorId(),
                        c.authorName(),
                        c.authorAvatarUrl(),
                        c.text(),
                        c.createdAt(),
                        false
                ))
                .toList();
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(commentsKey(videoUuid), json, COMMENTS_TTL);
        } catch (JsonProcessingException e) {
            return;
        }
    }

    private void evictCommentsCache(UUID videoUuid) {
        if (videoUuid == null) {
            return;
        }
        redisTemplate.delete(commentsKey(videoUuid));
    }

    private String commentsKey(UUID videoUuid) {
        return COMMENTS_KEY_PREFIX + videoUuid;
    }

    private List<CommentDto> applyMine(List<CommentDto> cached, Long currentUserId) {
        if (cached == null) {
            return List.of();
        }
        return cached.stream()
                .map(c -> new CommentDto(
                        c.id(),
                        c.authorId(),
                        c.authorName(),
                        c.authorAvatarUrl(),
                        c.text(),
                        c.createdAt(),
                        Objects.equals(currentUserId, c.authorId())
                ))
                .toList();
    }
}
