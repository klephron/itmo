package ru.itmo.zavar.ticktokcoretest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.CommentDto;
import ru.itmo.zavar.ticktokcoretest.dto.CreateCommentRequest;
import ru.itmo.zavar.ticktokcoretest.security.Role;
import ru.itmo.zavar.ticktokcoretest.service.CommentService;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos/{videoUuid}/comments")
public class CommentController {

    private final CommentService commentService;

    private boolean isModeratorOrAdmin(Authentication auth) {
        if (auth == null) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityBeans.UserPrincipal up) {
            return up.getRoles().contains(Role.ADMIN) || up.getRoles().contains(Role.MODERATOR);
        }
        return false;
    }

    @GetMapping
    public ResponseEntity<List<CommentDto>> list(
            @PathVariable UUID videoUuid,
            Authentication auth) {

        Long currentUserId = null;
        if (auth != null && auth.getPrincipal() instanceof SecurityBeans.UserPrincipal up) {
            currentUserId = up.getId();
        }

        return ResponseEntity.ok(commentService.listComments(videoUuid, currentUserId));
    }

    @PostMapping
    public ResponseEntity<CommentDto> add(
            @PathVariable UUID videoUuid,
            @Valid @RequestBody CreateCommentRequest req,
            Authentication auth) {

        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(commentService.addComment(videoUuid, req, auth));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID videoUuid,
            @PathVariable Long commentId,
            Authentication auth) {

        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        commentService.deleteComment(videoUuid, commentId, auth, isModeratorOrAdmin(auth));
        return ResponseEntity.noContent().build();
    }
}

