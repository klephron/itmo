package ru.itmo.zavar.brainstop.interactions.video.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;

import java.util.UUID;

@FeignClient(name = "video-service", contextId = "videoMetadataInternalClient", path = "/internal/videos/metadata")
public interface VideoMetadataInternalClient {

    @GetMapping("/{uuid}")
    VideoMetadataSummary getByUuid(@PathVariable("uuid") UUID uuid);

    @PostMapping("/{id}/likes")
    void updateLikes(@PathVariable("id") Long id, @RequestParam("count") long count);

    @PostMapping("/{id}/views")
    void updateViews(@PathVariable("id") Long id, @RequestParam("count") long count);

    @PostMapping("/{id}/likes/increment")
    Long incrementLikes(@PathVariable("id") Long id, @RequestParam("delta") long delta);

    @PostMapping("/{id}/views/increment")
    Long incrementViews(@PathVariable("id") Long id, @RequestParam("delta") long delta);
}
