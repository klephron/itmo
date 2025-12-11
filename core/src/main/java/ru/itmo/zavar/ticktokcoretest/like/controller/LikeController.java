package ru.itmo.zavar.ticktokcoretest.like.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.like.dto.response.LikeResponse;
import ru.itmo.zavar.ticktokcoretest.like.service.LikeService;
import ru.itmo.zavar.ticktokcoretest.common.security.CurrentUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
// MSPLIT: HTTP API for future Like/Metrics service
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> like(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        long count = likeService.likeVideo(uuid, principal.getId());
        return ResponseEntity.ok(new LikeResponse(count, true));
    }

    @DeleteMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> unlike(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        long count = likeService.unlikeVideo(uuid, principal.getId());
        return ResponseEntity.ok(new LikeResponse(count, false));
    }

    @GetMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> getLikeStatus(@PathVariable UUID uuid, @AuthenticationPrincipal CurrentUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        boolean liked = likeService.isLikedByUser(uuid, principal.getId());
        return ResponseEntity.ok(new LikeResponse(null, liked));
    }
}
