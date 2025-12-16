package ru.itmo.zavar.brainstop.interactions.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.interactions.client.CommentInternalClient;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos/{videoUuid}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Комментарии (interactions-service)")
public class CommentExternalController {

    private final CommentInternalClient commentInternalClient;

    private boolean isModeratorOrAdmin(CurrentUser principal) {
        if (principal == null) return false;
        return principal.getRoles().contains(Role.ADMIN) || principal.getRoles().contains(Role.MODERATOR);
    }

    @GetMapping
    @Operation(summary = "Получить комментарии видео")
    public ResponseEntity<List<CommentDto>> list(@PathVariable UUID videoUuid, @AuthenticationPrincipal CurrentUser principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(this.commentInternalClient.list(videoUuid, currentUserId));
    }

    @PostMapping
    @Operation(summary = "Добавить комментарий")
    public ResponseEntity<CommentDto> add(@PathVariable UUID videoUuid, @Valid @RequestBody CreateCommentRequest req, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String displayName = principal.getDisplayName();
        String avatarUrl = principal.getAvatarUrl();
        CommentDto dto = this.commentInternalClient.add(videoUuid, principal.getId(), displayName, avatarUrl, req);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Удалить комментарий")
    public ResponseEntity<Void> delete(@PathVariable UUID videoUuid, @PathVariable Long commentId, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        this.commentInternalClient.delete(videoUuid, commentId, principal.getId(), isModeratorOrAdmin(principal));
        return ResponseEntity.noContent().build();
    }
}
