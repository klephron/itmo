package ru.itmo.zavar.brainstop.video.controller;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;

import java.util.UUID;

@RestController
@RequestMapping("/internal/videos/metadata")
@RequiredArgsConstructor
@Tag(name = "Метаданные видео (внутренние)", description = "Внутренние эндпоинты метаданных видео")
public class VideoMetadataInternalController {

    private final VideoMetadataPublicApi videoMetadataPublicApi;

    @GetMapping("/{uuid}")
    @Operation(summary = "Сводка метаданных по UUID видео")
    public ResponseEntity<VideoMetadataSummary> getSummary(@PathVariable UUID uuid) {
        return this.videoMetadataPublicApi.findByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/likes/increment")
    @Operation(summary = "Увеличить счётчик лайков на delta")
    public ResponseEntity<Long> incrementLikes(@PathVariable Long id, @RequestParam("delta") long delta) {
        long updated = this.videoMetadataPublicApi.incrementLikesCount(id, delta);
        if (updated < 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/views/increment")
    @Operation(summary = "Увеличить счётчик просмотров на delta")
    public ResponseEntity<Long> incrementViews(@PathVariable Long id, @RequestParam("delta") long delta) {
        long updated = this.videoMetadataPublicApi.incrementViewsCount(id, delta);
        if (updated < 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/likes")
    @Operation(summary = "Установить счётчик лайков")
    public ResponseEntity<Void> updateLikes(@PathVariable Long id, @RequestParam("count") long count) {
        this.videoMetadataPublicApi.updateLikesCount(id, count);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/views")
    @Operation(summary = "Установить счётчик просмотров")
    public ResponseEntity<Void> updateViews(@PathVariable Long id, @RequestParam("count") long count) {
        this.videoMetadataPublicApi.updateViewsCount(id, count);
        return ResponseEntity.ok().build();
    }
}
