package ru.itmo.zavar.brainstop.interactions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.comment.service.CommentService;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.common.security.Role;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos/{videoUuid}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Комментарии (interactions-service)")
public class CommentExternalController {

    private final CommentService commentService;

    private boolean isModeratorOrAdmin(CurrentUser principal) {
        if (principal == null) return false;
        return principal.getRoles().contains(Role.ADMIN) || principal.getRoles().contains(Role.MODERATOR);
    }

    @GetMapping
    @Operation(summary = "Получить комментарии видео")
    public ResponseEntity<Page<CommentDto>> list(@PathVariable UUID videoUuid,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @AuthenticationPrincipal CurrentUser principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(this.commentService.listComments(videoUuid, currentUserId, PageRequest.of(Math.max(0, page), Math.max(1, size))));
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "Получить комментарий")
    public ResponseEntity<CommentDto> getById(@PathVariable UUID videoUuid, @PathVariable Long commentId, @AuthenticationPrincipal CurrentUser principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(this.commentService.findComment(videoUuid, commentId, currentUserId));
    }

    @PostMapping
    @Operation(summary = "Добавить комментарий")
    public ResponseEntity<CommentDto> add(@PathVariable UUID videoUuid, @Valid @RequestBody CreateCommentRequest req, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String displayName = principal.getDisplayName();
        String avatarUrl = principal.getAvatarUrl();
        CommentDto dto = this.commentService.addComment(videoUuid, req, principal.getId(), displayName, avatarUrl);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Удалить комментарий")
    public ResponseEntity<Void> delete(@PathVariable UUID videoUuid, @PathVariable Long commentId, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        this.commentService.deleteComment(videoUuid, commentId, principal.getId(), isModeratorOrAdmin(principal));
        return ResponseEntity.noContent().build();
    }
}
