package ru.itmo.zavar.ticktokcoretest.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.common.security.Role;
import ru.itmo.zavar.ticktokcoretest.comment.dto.CommentDto;
import ru.itmo.zavar.ticktokcoretest.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.ticktokcoretest.comment.service.CommentService;
import ru.itmo.zavar.ticktokcoretest.common.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos/{videoUuid}/comments")
// MSPLIT: boundary for future Comment Service API
public class CommentController {

    private final CommentService commentService;

    private boolean isModeratorOrAdmin(CurrentUser principal) {
        if (principal == null) return false;
        return principal.getRoles().contains(Role.ADMIN) || principal.getRoles().contains(Role.MODERATOR);
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> list(@PathVariable UUID videoUuid, @AuthenticationPrincipal CurrentUser principal) {
        Long currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(this.commentService.listComments(videoUuid, currentUserId));
    }

    @PostMapping
    public ResponseEntity<CommentDto> add(@PathVariable UUID videoUuid, @Valid @RequestBody CreateCommentRequest req, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String displayName = principal.getDisplayName();
        String avatarUrl = principal.getAvatarUrl();
        return ResponseEntity.ok(this.commentService.addComment(videoUuid, req, principal.getId(), displayName, avatarUrl));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID videoUuid, @PathVariable Long commentId, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        this.commentService.deleteComment(videoUuid, commentId, principal.getId(), isModeratorOrAdmin(principal));
        return ResponseEntity.noContent().build();
    }
}

