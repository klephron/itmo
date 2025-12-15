package ru.itmo.zavar.ticktokcoretest.interactions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.ticktokcoretest.common.security.CurrentUser;
import ru.itmo.zavar.ticktokcoretest.interactions.client.LikeInternalClient;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.LikeResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "Лайки (interactions-service)")
public class LikeExternalController {

    private final LikeInternalClient likeInternalClient;

    @PostMapping("/{uuid}/like")
    @Operation(summary = "Поставить лайк видео")
    public ResponseEntity<LikeResponse> like(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        LikeResponse resp = likeInternalClient.like(uuid, principal.getId());
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{uuid}/like")
    @Operation(summary = "Снять лайк видео")
    public ResponseEntity<LikeResponse> unlike(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        LikeResponse resp = likeInternalClient.unlike(uuid, principal.getId());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{uuid}/like")
    @Operation(summary = "Проверить статус лайка и число лайков")
    public ResponseEntity<LikeResponse> getLikeStatus(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        LikeResponse resp = likeInternalClient.status(uuid, principal.getId());
        return ResponseEntity.ok(resp);
    }
}
