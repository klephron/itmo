package ru.itmo.zavar.brainstop.interactions.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.comment.dto.CommentDto;
import ru.itmo.zavar.brainstop.comment.dto.CreateCommentRequest;
import ru.itmo.zavar.brainstop.comment.service.CommentService;
import ru.itmo.zavar.brainstop.common.security.Role;
import ru.itmo.zavar.brainstop.like.dto.request.WatchProgressRequest;
import ru.itmo.zavar.brainstop.like.dto.response.LikeResponse;
import ru.itmo.zavar.brainstop.like.dto.response.ViewResponse;
import ru.itmo.zavar.brainstop.like.service.LikeService;
import ru.itmo.zavar.brainstop.like.service.ViewService;
import ru.itmo.zavar.brainstop.like.service.WatchService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/internal/interactions")
@RequiredArgsConstructor
@Tag(name = "Internal-Interactions", description = "Внутренний API для сервиса взаимодействий")
public class InteractionsInternalController {

    private final CommentService commentService;
    private final LikeService likeService;
    private final ViewService viewService;
    private final WatchService watchService;

    @GetMapping("/videos/{uuid}/comments")
    @Operation(summary = "Внутренний: список комментариев")
    public List<CommentDto> listComments(@PathVariable UUID uuid,
                                         @RequestParam(value = "userId", required = false) Long userId) {
        return this.commentService.listComments(uuid, userId);
    }

    @PostMapping("/videos/{uuid}/comments")
    @Operation(summary = "Внутренний: добавить комментарий")
    public CommentDto addComment(@PathVariable UUID uuid,
                                 @RequestParam("userId") Long userId,
                                 @RequestParam("displayName") String displayName,
                                 @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                                 @Valid @RequestBody CreateCommentRequest req) {
        return this.commentService.addComment(uuid, req, userId, displayName, avatarUrl);
    }

    @DeleteMapping("/videos/{uuid}/comments/{commentId}")
    @Operation(summary = "Внутренний: удалить комментарий")
    public void deleteComment(@PathVariable UUID uuid,
                              @PathVariable Long commentId,
                              @RequestParam("userId") Long userId,
                              @RequestParam("moderator") boolean moderator) {
        this.commentService.deleteComment(uuid, commentId, userId, moderator);
    }

    @PostMapping("/videos/{uuid}/like")
    @Operation(summary = "Внутренний: поставить лайк")
    public LikeResponse like(@PathVariable UUID uuid, @RequestParam("userId") Long userId) {
        long count = likeService.likeVideo(uuid, userId);
        return new LikeResponse(count, true);
    }

    @DeleteMapping("/videos/{uuid}/like")
    @Operation(summary = "Внутренний: снять лайк")
    public LikeResponse unlike(@PathVariable UUID uuid, @RequestParam("userId") Long userId) {
        long count = likeService.unlikeVideo(uuid, userId);
        return new LikeResponse(count, false);
    }

    @GetMapping("/videos/{uuid}/like")
    @Operation(summary = "Внутренний: статус лайка/число лайков")
    public LikeResponse status(@PathVariable UUID uuid, @RequestParam("userId") Long userId) {
        boolean liked = likeService.isLikedByUser(uuid, userId);
        // лайки считаются в метаданных; null count означает не запрашивали обновление
        return new LikeResponse(null, liked);
    }

    @PostMapping("/videos/{uuid}/view")
    @Operation(summary = "Внутренний: отметить просмотр")
    public ViewResponse view(@PathVariable UUID uuid,
                             @RequestParam(value = "userId", required = false) Long userId) {
        long count = this.viewService.viewVideo(uuid, userId);
        return new ViewResponse(count);
    }

    @GetMapping("/videos/{uuid}/view")
    @Operation(summary = "Внутренний: получить число просмотров")
    public ViewResponse count(@PathVariable UUID uuid) {
        return new ViewResponse(this.viewService.getViews(uuid));
    }

    @PostMapping("/videos/{uuid}/watch")
    @Operation(summary = "Внутренний: зафиксировать прогресс просмотра")
    public void watch(@PathVariable UUID uuid,
                      @RequestParam(value = "userId", required = false) Long userId,
                      @RequestBody WatchProgressRequest request) {
        this.watchService.recordProgress(uuid, userId, request);
    }
}
