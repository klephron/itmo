package ru.itmo.zavar.ticktokcoretest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.security.SecurityBeans;
import ru.itmo.zavar.ticktokcoretest.service.LikeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> like(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = extractUserId(authentication);
        long count = likeService.likeVideo(uuid, userId);
        return ResponseEntity.ok(new LikeResponse(count, true));
    }

    @DeleteMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> unlike(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = extractUserId(authentication);
        long count = likeService.unlikeVideo(uuid, userId);
        return ResponseEntity.ok(new LikeResponse(count, false));
    }

    @GetMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> getLikeStatus(@PathVariable UUID uuid, Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean liked = likeService.isLikedByUser(uuid, userId);
        // Количество лайков здесь не возвращаем (null), т.к. фронт берет его из списка видео
        return ResponseEntity.ok(new LikeResponse(null, liked));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Unauthenticated");
        }

        // Исправлено: берем Principal напрямую
        if (authentication.getPrincipal() instanceof SecurityBeans.UserPrincipal up) {
            return up.getId();
        }

        throw new RuntimeException("Unknown principal type: " + authentication.getPrincipal().getClass());
    }

    public record LikeResponse(Long likesCount, Boolean liked) {}
}