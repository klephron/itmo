package ru.itmo.zavar.brainstop.interactions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.security.CurrentUser;
import ru.itmo.zavar.brainstop.like.dto.response.LikeResponse;
import ru.itmo.zavar.brainstop.like.service.LikeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "Лайки (interactions-service)")
public class LikeExternalController {

    private final LikeService likeService;

    @PostMapping("/{uuid}/like")
    @Operation(summary = "Поставить лайк видео")
    public ResponseEntity<LikeResponse> like(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        long count = likeService.likeVideo(uuid, principal.getId());
        return ResponseEntity.ok(new LikeResponse(count, true));
    }

    @DeleteMapping("/{uuid}/like")
    @Operation(summary = "Снять лайк видео")
    public ResponseEntity<LikeResponse> unlike(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        long count = likeService.unlikeVideo(uuid, principal.getId());
        return ResponseEntity.ok(new LikeResponse(count, false));
    }

    @GetMapping("/{uuid}/like")
    @Operation(summary = "Проверить статус лайка и число лайков")
    public ResponseEntity<LikeResponse> getLikeStatus(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        boolean liked = principal != null && likeService.isLikedByUser(uuid, principal.getId());
        long count = likeService.getCount(uuid);
        return ResponseEntity.ok(new LikeResponse(count, liked));
    }
}
