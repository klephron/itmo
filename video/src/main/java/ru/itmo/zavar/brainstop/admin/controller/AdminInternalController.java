package ru.itmo.zavar.brainstop.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.video.VideoAdminApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoModerationApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/admin")
@RequiredArgsConstructor
@Tag(name = "Администрирование видео (внутреннее)", description = "Внутренние операции администрирования и модерации видео")
public class AdminInternalController {

    private final VideoAdminApi videoAdminApi;
    private final VideoModerationApi videoModerationApi;

    @PostMapping("/thumbnails/rebuild")
    @Operation(summary = "Пересоздать превью всех видео")
    public ResponseEntity<Map<String, Object>> regenerateThumbnails(@RequestParam(name = "force", defaultValue = "true") boolean force) {
        int updated = this.videoAdminApi.regenerateThumbnails(force);
        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "force", force
        ));
    }

    @PostMapping("/embeddings/rebuild")
    @Operation(summary = "Пересчитать эмбеддинги видео")
    public ResponseEntity<Map<String, Object>> rebuildEmbeddings() {
        int updated = this.videoAdminApi.rebuildEmbeddings();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @GetMapping("/users/{id}/videos")
    @Operation(summary = "Список видео пользователя для модерации")
    public ResponseEntity<List<UserVideoDto>> getUserVideos(@PathVariable Long id) {
        return ResponseEntity.ok(this.videoModerationApi.findByUploaderId(id));
    }

    @DeleteMapping("/videos/{uuid}")
    @Operation(summary = "Удалить видео (для модерации)")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID uuid) {
        this.videoAdminApi.deleteVideo(uuid);
        return ResponseEntity.noContent().build();
    }
}
