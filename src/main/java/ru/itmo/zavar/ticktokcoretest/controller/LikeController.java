package ru.itmo.zavar.ticktokcoretest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.ticktokcoretest.service.LikeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    // лайк
    @PostMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> like(@PathVariable UUID uuid) {
        Long userId = extractUserId(); // см. ниже
        long count = likeService.likeVideo(uuid, userId);
        boolean liked = true;
        return ResponseEntity.ok(new LikeResponse(count, liked));
    }

    // снять лайк
    @DeleteMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> unlike(@PathVariable UUID uuid) {
        Long userId = extractUserId();
        long count = likeService.unlikeVideo(uuid, userId);
        boolean liked = false;
        return ResponseEntity.ok(new LikeResponse(count, liked));
    }

    // проверить, лайкнуто ли текущим пользователем
    @GetMapping("/{uuid}/like")
    public ResponseEntity<LikeResponse> getLikeStatus(@PathVariable UUID uuid) {
        Long userId = extractUserId();
        boolean liked = likeService.isLikedByUser(uuid, userId);
        // счётчик можно получить через FileMetadataRepo или вернуть как есть,
        // но проще дать фронту только флаг, а счётчик брать из /streaming/video/list
        return ResponseEntity.ok(new LikeResponse(null, liked));
    }

//    private Long extractUserId(Authentication authentication) {
//        // тут подстрой под свою реализацию безопасности
//        // пример, если ты кладёшь id в attributes:
//        if (authentication == null) {
//            throw new RuntimeException("Unauthenticated");
//        }
//        Object raw = authentication.getAttributes().get("id");
//        if (raw instanceof Long l) return l;
//        if (raw instanceof Integer i) return i.longValue();
//        return Long.valueOf(raw.toString());
//    }
    private Long extractUserId() {
        return 1L; // временный заглушечный юзер
    }

    public record LikeResponse(Long likesCount, Boolean liked) {}
}

