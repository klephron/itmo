package ru.itmo.zavar.ticktokcoretest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.ticktokcoretest.dto.CommentDto;
import ru.itmo.zavar.ticktokcoretest.dto.request.CreateCommentRequest;
import ru.itmo.zavar.ticktokcoretest.entity.CommentEntity;
import ru.itmo.zavar.ticktokcoretest.entity.UserEntity;
import ru.itmo.zavar.ticktokcoretest.repo.CommentRepository;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.UserRepository;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    private Long extractUserId(Authentication auth) {
        if (auth == null) throw new RuntimeException("Unauthenticated");
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getId();
        }
        throw new RuntimeException("Cannot extract user id");
    }

    private String extractAvatarUrl(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getAvatarUrl();
        }
        return null;
    }

    private String extractDisplayName(Authentication auth) {
        if (auth == null) return "Аноним";
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getDisplayName();
        }
        return "Аноним";
    }

    @Transactional(readOnly = true)
    public List<CommentDto> listComments(UUID videoUuid, Long currentUserId) {
        fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoUuid));

        List<CommentEntity> comments =
                commentRepository.findByVideoUuidOrderByCreatedAtAsc(videoUuid);

        // 🔹 одним запросом забираем всех авторов, чтобы достать avatarUrl
        Set<Long> authorIds = comments.stream()
                .map(CommentEntity::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserEntity> userMap = authorIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        return comments.stream()
                .map(c -> {
                    UserEntity user = c.getAuthorId() == null ? null : userMap.get(c.getAuthorId());
                    String avatarUrl = (user != null) ? (user.getAvatarKey() == null
                            ? null
                            : "/api/users/" + user.getId() + "/avatar") : null;
                    return new CommentDto(
                            c.getId(),
                            c.getAuthorId(),
                            c.getAuthorName(),
                            avatarUrl,
                            c.getText(),
                            c.getCreatedAt(),
                            currentUserId != null && currentUserId.equals(c.getAuthorId())
                    );
                })
                .toList();
    }

    @Transactional
    public CommentDto addComment(UUID videoUuid,
                                 CreateCommentRequest req,
                                 Authentication auth) {

        fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoUuid));

        Long userId = extractUserId(auth);
        String displayName = extractDisplayName(auth);
        String avatarUrl = extractAvatarUrl(auth);

        CommentEntity entity = new CommentEntity();
        entity.setVideoUuid(videoUuid);
        entity.setAuthorId(userId);
        entity.setAuthorName(displayName);
        entity.setText(req.text());
        entity.setCreatedAt(Instant.now());

        entity = commentRepository.save(entity);

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
    public void deleteComment(UUID videoUuid, Long commentId, Authentication auth, boolean isModeratorOrAdmin) {
        CommentEntity entity = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!entity.getVideoUuid().equals(videoUuid)) {
            throw new IllegalArgumentException("Comment does not belong to this video");
        }

        Long currentUserId = extractUserId(auth);

        if (!isModeratorOrAdmin && !entity.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Forbidden");
        }

        commentRepository.delete(entity);
    }
}
