package ru.itmo.zavar.brainstop.video.controller;

import lombok.RequiredArgsConstructor;
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
public class VideoMetadataInternalController {

    private final VideoMetadataPublicApi videoMetadataPublicApi;

    @GetMapping("/{uuid}")
    public ResponseEntity<VideoMetadataSummary> getSummary(@PathVariable UUID uuid) {
        return this.videoMetadataPublicApi.findByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> updateLikes(@PathVariable Long id, @RequestParam("count") long count) {
        this.videoMetadataPublicApi.updateLikesCount(id, count);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/views")
    public ResponseEntity<Void> updateViews(@PathVariable Long id, @RequestParam("count") long count) {
        this.videoMetadataPublicApi.updateViewsCount(id, count);
        return ResponseEntity.ok().build();
    }
}
