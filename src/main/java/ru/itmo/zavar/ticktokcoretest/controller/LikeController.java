package ru.itmo.zavar.ticktokcoretest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.dto.response.LikeResponse;
import ru.itmo.zavar.ticktokcoretest.service.AuthService;
import ru.itmo.zavar.ticktokcoretest.service.LikeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> like(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = AuthService.extractUserId(authentication);
        long count = likeService.likeVideo(uuid, userId);
        return ResponseEntity.ok(new LikeResponse(count, true));
    }

    @DeleteMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> unlike(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = AuthService.extractUserId(authentication);
        long count = likeService.unlikeVideo(uuid, userId);
        return ResponseEntity.ok(new LikeResponse(count, false));
    }

    @GetMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> getLikeStatus(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = AuthService.extractUserId(authentication);
        boolean liked = likeService.isLikedByUser(uuid, userId);
        return ResponseEntity.ok(new LikeResponse(null, liked));
    }
}