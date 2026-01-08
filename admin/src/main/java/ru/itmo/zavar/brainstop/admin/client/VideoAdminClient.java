package ru.itmo.zavar.brainstop.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "video-service", contextId = "videoAdminClient", path = "/internal/admin", fallbackFactory = VideoAdminClientFallbackFactory.class)
public interface VideoAdminClient {

    @PostMapping("/thumbnails/rebuild")
    Map<String, Object> regenerateThumbnails(@RequestParam("force") boolean force);
}
